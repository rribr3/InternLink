package com.example.internlink;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced Search Manager to handle search history and search suggestions
 */
public class EnhancedSearchManager {
    private static final String SEARCH_PREFS = "enhanced_search_prefs";
    private static final String SEARCH_HISTORY_KEY = "search_history";
    private static final String POPULAR_SEARCHES_KEY = "popular_searches";
    private static final int MAX_HISTORY_SIZE = 15;
    private static final int MAX_POPULAR_SIZE = 10;

    private final SharedPreferences prefs;
    private List<String> searchHistory;
    private List<SearchSuggestion> popularSearches;

    public static class SearchSuggestion {
        public String query;
        public int frequency;
        public long lastUsed;

        public SearchSuggestion(String query, int frequency, long lastUsed) {
            this.query = query;
            this.frequency = frequency;
            this.lastUsed = lastUsed;
        }

        // Convert to JSON
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("query", query);
            json.put("frequency", frequency);
            json.put("lastUsed", lastUsed);
            return json;
        }

        // Create from JSON
        public static SearchSuggestion fromJson(JSONObject json) throws JSONException {
            return new SearchSuggestion(
                    json.getString("query"),
                    json.getInt("frequency"),
                    json.getLong("lastUsed")
            );
        }
    }

    public EnhancedSearchManager(Context context) {
        prefs = context.getSharedPreferences(SEARCH_PREFS, Context.MODE_PRIVATE);
        loadSearchData();
    }

    private void loadSearchData() {
        // Load search history
        loadSearchHistory();

        // Load popular searches
        loadPopularSearches();
    }

    private void loadSearchHistory() {
        String historyJson = prefs.getString(SEARCH_HISTORY_KEY, "[]");
        try {
            JSONArray jsonArray = new JSONArray(historyJson);
            searchHistory = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                searchHistory.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            searchHistory = new ArrayList<>();
        }
    }

    private void loadPopularSearches() {
        String popularJson = prefs.getString(POPULAR_SEARCHES_KEY, "[]");
        try {
            JSONArray jsonArray = new JSONArray(popularJson);
            popularSearches = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject suggestionJson = jsonArray.getJSONObject(i);
                popularSearches.add(SearchSuggestion.fromJson(suggestionJson));
            }
        } catch (JSONException e) {
            popularSearches = new ArrayList<>();
        }
    }

    public void addSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;

        query = query.trim();

        // Update search history
        searchHistory.remove(query); // Remove if exists to move to top
        searchHistory.add(0, query); // Add to beginning

        // Limit history size
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory = searchHistory.subList(0, MAX_HISTORY_SIZE);
        }

        // Update popular searches
        updatePopularSearches(query);

        // Save data
        saveSearchData();
    }

    private void updatePopularSearches(String query) {
        SearchSuggestion existing = null;
        for (SearchSuggestion suggestion : popularSearches) {
            if (suggestion.query.equalsIgnoreCase(query)) {
                existing = suggestion;
                break;
            }
        }

        if (existing != null) {
            existing.frequency++;
            existing.lastUsed = System.currentTimeMillis();
        } else {
            popularSearches.add(new SearchSuggestion(query, 1, System.currentTimeMillis()));
        }

        // Sort by frequency (descending) and then by recency
        popularSearches.sort((a, b) -> {
            if (a.frequency != b.frequency) {
                return Integer.compare(b.frequency, a.frequency);
            } else {
                return Long.compare(b.lastUsed, a.lastUsed);
            }
        });

        // Limit popular searches size
        if (popularSearches.size() > MAX_POPULAR_SIZE) {
            popularSearches = popularSearches.subList(0, MAX_POPULAR_SIZE);
        }
    }

    private void saveSearchData() {
        saveSearchHistory();
        savePopularSearches();
    }

    private void saveSearchHistory() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (String query : searchHistory) {
                jsonArray.put(query);
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SEARCH_HISTORY_KEY, jsonArray.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePopularSearches() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SearchSuggestion suggestion : popularSearches) {
                jsonArray.put(suggestion.toJson());
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(POPULAR_SEARCHES_KEY, jsonArray.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getSearchHistory() {
        return new ArrayList<>(searchHistory);
    }

    public List<String> getPopularSearches() {
        List<String> popular = new ArrayList<>();
        for (SearchSuggestion suggestion : popularSearches) {
            popular.add(suggestion.query);
        }
        return popular;
    }

    public void removeFromHistory(String query) {
        searchHistory.remove(query);
        saveSearchHistory();
    }

    public void clearSearchHistory() {
        searchHistory.clear();
        saveSearchHistory();
    }

    public void clearPopularSearches() {
        popularSearches.clear();
        savePopularSearches();
    }

    public void clearAllSearchData() {
        searchHistory.clear();
        popularSearches.clear();
        saveSearchData();
    }

    /**
     * Get search suggestions based on current input
     * @param input Current search input
     * @return List of suggested queries
     */
    public List<String> getSearchSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();

        if (input == null || input.trim().isEmpty()) {
            // Return recent searches when no input
            suggestions.addAll(getSearchHistory());
            return suggestions.subList(0, Math.min(suggestions.size(), 5));
        }

        String lowercaseInput = input.toLowerCase().trim();

        // First, add matching history items
        for (String historyItem : searchHistory) {
            if (historyItem.toLowerCase().contains(lowercaseInput) &&
                    !suggestions.contains(historyItem)) {
                suggestions.add(historyItem);
            }
        }

        // Then, add matching popular searches
        for (String popularItem : getPopularSearches()) {
            if (popularItem.toLowerCase().contains(lowercaseInput) &&
                    !suggestions.contains(popularItem)) {
                suggestions.add(popularItem);
            }
        }

        // Limit suggestions
        return suggestions.subList(0, Math.min(suggestions.size(), 8));
    }

    /**
     * Get predefined search categories/tags
     */
    public List<String> getSearchCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("Software Development");
        categories.add("Data Science");
        categories.add("Machine Learning");
        categories.add("Web Development");
        categories.add("Mobile Development");
        categories.add("UI/UX Design");
        categories.add("Digital Marketing");
        categories.add("Business Analysis");
        categories.add("Project Management");
        categories.add("Cybersecurity");
        categories.add("Cloud Computing");
        categories.add("DevOps");
        return categories;
    }

    /**
     * Get search analytics
     */
    public int getTotalSearches() {
        int total = 0;
        for (SearchSuggestion suggestion : popularSearches) {
            total += suggestion.frequency;
        }
        return total;
    }

    /**
     * Get most popular search query
     */
    public String getMostPopularSearch() {
        if (popularSearches.isEmpty()) return null;
        return popularSearches.get(0).query;
    }

    /**
     * Check if a query exists in history
     */
    public boolean isInHistory(String query) {
        return searchHistory.contains(query);
    }

    /**
     * Get recent searches (last N items)
     */
    public List<String> getRecentSearches(int count) {
        int limit = Math.min(count, searchHistory.size());
        return new ArrayList<>(searchHistory.subList(0, limit));
    }
}