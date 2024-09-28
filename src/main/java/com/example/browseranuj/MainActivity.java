package com.example.browseranuj;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Set<Pattern> blockedPatterns = new HashSet<>();
    private Set<String> whitelistUrls = new HashSet<>(Arrays.asList("google.com")); // Add more URLs as needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);


        new LoadEasyListTask().execute();

        // Set custom WebViewClient to intercept and block ad requests
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // Log URL for debugging
                Log.d("Request URL", url);


                if (isWhitelisted(url)) {
                    return super.shouldInterceptRequest(view, request);
                }


                for (Pattern pattern : blockedPatterns) {
                    if (pattern.matcher(url).find()) {
                        // Log blocked URL
                        Log.d("Blocked URL", url);
                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            private boolean isWhitelisted(String url) {
                for (String whitelistUrl : whitelistUrls) {
                    if (url.contains(whitelistUrl)) {
                        return true;
                    }
                }
                return false;
            }
        });

        // Load a website
        webView.loadUrl("https://www.google.com/");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.clearCache(true);
            webView = null;
        }
    }

    // Background task to load EasyList filter rules
    private class LoadEasyListTask extends AsyncTask<Void, Void, Set<Pattern>> {
        @Override
        protected Set<Pattern> doInBackground(Void... voids) {
            return loadEasyListFromRaw();
        }

        @Override
        protected void onPostExecute(Set<Pattern> result) {
            blockedPatterns = result;
        }
    }

    // Method to load EasyList filters from the raw resource file
    private Set<Pattern> loadEasyListFromRaw() {
        Set<Pattern> patterns = new HashSet<>();
        try {
            // Open the EasyList text file from res/raw
            InputStream inputStream = getResources().openRawResource(R.raw.easylist);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            // Read each line and extract blocked patterns
            while ((line = reader.readLine()) != null) {
                // Extract domains or patterns from EasyList
                if (line.startsWith("||")) {
                    String pattern = extractPatternFromFilter(line);
                    if (pattern != null) {
                        patterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return patterns;
    }

    // Helper method to extract pattern from EasyList filter
    private String extractPatternFromFilter(String filterLine) {
        // Extract patterns starting with "||" (used in EasyList)
        if (filterLine.startsWith("||")) {
            return filterLine.substring(2).split("/")[0].replace(".", "\\.").replace("*", ".*");
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
