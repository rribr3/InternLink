package com.example.internlink;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

/**
 * Enhanced Search Manager to handle search history and search suggestions
 */
public class EnhancedSearchManager {
    private static final String TAG = "EnhancedSearchManager";
    private static final String SEARCH_PREFS = "enhanced_search_prefs";
    private static final String SEARCH_HISTORY_KEY = "search_history";
    private static final String POPULAR_SEARCHES_KEY = "popular_searches";
    private static final int MAX_HISTORY_SIZE = 15;
    private static final int MAX_POPULAR_SIZE = 10;

    private final SharedPreferences prefs;
    private List<String> searchHistory;
    private List<SearchSuggestion> popularSearches;
    private List<String> searchCategories = new ArrayList<>();
    private CategoryLoadCallback categoryLoadCallback;

    public interface CategoryLoadCallback {
        void onCategoriesLoaded(List<String> categories);
    }

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

    public void loadSearchCategories(CategoryLoadCallback callback) {
        this.categoryLoadCallback = callback;
        DatabaseReference categoriesRef = FirebaseDatabase.getInstance().getReference("categories");

        categoriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                searchCategories.clear();

                try {
                    for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        // ✅ FIXED: Safe type checking instead of direct Boolean.class conversion
                        Object categoryValue = categorySnapshot.getValue();
                        boolean isActive = false;

                        if (categoryValue instanceof Boolean) {
                            isActive = (Boolean) categoryValue;
                        } else if (categoryValue instanceof String) {
                            String strValue = (String) categoryValue;
                            isActive = "true".equalsIgnoreCase(strValue) || "1".equals(strValue) || "active".equalsIgnoreCase(strValue);
                        } else if (categoryValue instanceof Number) {
                            Number numValue = (Number) categoryValue;
                            isActive = numValue.intValue() != 0;
                        } else if (categoryValue instanceof HashMap) {
                            // Handle case where category is stored as a map
                            HashMap<String, Object> categoryMap = (HashMap<String, Object>) categoryValue;
                            Object activeValue = categoryMap.get("active");
                            if (activeValue instanceof Boolean) {
                                isActive = (Boolean) activeValue;
                            } else if (activeValue instanceof String) {
                                isActive = "true".equalsIgnoreCase((String) activeValue);
                            }
                            // If no "active" field, assume it's active if the map exists
                            else if (activeValue == null) {
                                isActive = true;
                            }
                        } else if (categoryValue != null) {
                            // If it's any other non-null value, consider it active
                            isActive = true;
                        }

                        // Only add categories that are marked as active
                        if (isActive) {
                            String categoryKey = categorySnapshot.getKey();
                            if (categoryKey != null && !categoryKey.trim().isEmpty()) {
                                searchCategories.add(categoryKey);
                            }
                        }
                    }

                    // If no categories found from Firebase, add default categories
                    if (searchCategories.isEmpty()) {
                        addDefaultCategories();
                    }

                    // Sort categories alphabetically for better display
                    Collections.sort(searchCategories);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing categories: " + e.getMessage());
                    // Fallback to default categories
                    addDefaultCategories();
                }

                if (categoryLoadCallback != null) {
                    categoryLoadCallback.onCategoriesLoaded(new ArrayList<>(searchCategories));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Categories loading cancelled: " + error.getMessage());
                // In case of error, return default categories
                addDefaultCategories();
                if (categoryLoadCallback != null) {
                    categoryLoadCallback.onCategoriesLoaded(new ArrayList<>(searchCategories));
                }
            }
        });
    }

    /**
     * Add default search categories when Firebase data is unavailable
     */
    private void addDefaultCategories() {
        searchCategories.clear();
        searchCategories.add("Technology");
        searchCategories.add("Engineering");
        searchCategories.add("Marketing");
        searchCategories.add("Finance");
        searchCategories.add("Design");
        searchCategories.add("Data Science");
        searchCategories.add("Business");
        searchCategories.add("Healthcare");
        searchCategories.add("Education");
        searchCategories.add("Research");
        Collections.sort(searchCategories);
    }

    private void loadSearchHistory() {
        String historyJson = prefs.getString(SEARCH_HISTORY_KEY, "[]");
        try {
            JSONArray jsonArray = new JSONArray(historyJson);
            searchHistory = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String query = jsonArray.getString(i);
                if (query != null && !query.trim().isEmpty()) {
                    searchHistory.add(query);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading search history: " + e.getMessage());
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
                SearchSuggestion suggestion = SearchSuggestion.fromJson(suggestionJson);
                if (suggestion != null && suggestion.query != null && !suggestion.query.trim().isEmpty()) {
                    popularSearches.add(suggestion);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading popular searches: " + e.getMessage());
            popularSearches = new ArrayList<>();
        }
    }

    public void addSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;

        query = query.trim();

        try {
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

        } catch (Exception e) {
            Log.e(TAG, "Error adding search query: " + e.getMessage());
        }
    }

    private void updatePopularSearches(String query) {
        try {
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

        } catch (Exception e) {
            Log.e(TAG, "Error updating popular searches: " + e.getMessage());
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
                if (query != null && !query.trim().isEmpty()) {
                    jsonArray.put(query);
                }
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SEARCH_HISTORY_KEY, jsonArray.toString());
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving search history: " + e.getMessage());
        }
    }

    private void savePopularSearches() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SearchSuggestion suggestion : popularSearches) {
                if (suggestion != null && suggestion.query != null && !suggestion.query.trim().isEmpty()) {
                    jsonArray.put(suggestion.toJson());
                }
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(POPULAR_SEARCHES_KEY, jsonArray.toString());
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving popular searches: " + e.getMessage());
        }
    }

    public List<String> getSearchHistory() {
        return new ArrayList<>(searchHistory != null ? searchHistory : new ArrayList<>());
    }

    public List<String> getPopularSearches() {
        List<String> popular = new ArrayList<>();
        if (popularSearches != null) {
            for (SearchSuggestion suggestion : popularSearches) {
                if (suggestion != null && suggestion.query != null && !suggestion.query.trim().isEmpty()) {
                    popular.add(suggestion.query);
                }
            }
        }
        return popular;
    }

    public void removeFromHistory(String query) {
        if (searchHistory != null && query != null) {
            searchHistory.remove(query);
            saveSearchHistory();
        }
    }

    public void clearSearchHistory() {
        if (searchHistory != null) {
            searchHistory.clear();
            saveSearchHistory();
        }
    }

    public void clearPopularSearches() {
        if (popularSearches != null) {
            popularSearches.clear();
            savePopularSearches();
        }
    }

    public void clearAllSearchData() {
        if (searchHistory != null) {
            searchHistory.clear();
        }
        if (popularSearches != null) {
            popularSearches.clear();
        }
        saveSearchData();
    }

    /**
     * Get search suggestions based on current input
     * @param input Current search input
     * @return List of suggested queries
     */
    public List<String> getSearchSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();

        try {
            if (input == null || input.trim().isEmpty()) {
                // Return recent searches when no input
                suggestions.addAll(getSearchHistory());
                return suggestions.subList(0, Math.min(suggestions.size(), 5));
            }

            String lowercaseInput = input.toLowerCase().trim();

            // First, add matching history items
            for (String historyItem : getSearchHistory()) {
                if (historyItem != null && historyItem.toLowerCase().contains(lowercaseInput) &&
                        !suggestions.contains(historyItem)) {
                    suggestions.add(historyItem);
                }
            }

            // Then, add matching popular searches
            for (String popularItem : getPopularSearches()) {
                if (popularItem != null && popularItem.toLowerCase().contains(lowercaseInput) &&
                        !suggestions.contains(popularItem)) {
                    suggestions.add(popularItem);
                }
            }

            // Limit suggestions
            return suggestions.subList(0, Math.min(suggestions.size(), 8));

        } catch (Exception e) {
            Log.e(TAG, "Error getting search suggestions: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get predefined search categories/tags
     */
    public List<String> getSearchCategories() {
        return new ArrayList<>(searchCategories != null ? searchCategories : new ArrayList<>());
    }

    /**
     * Get search analytics
     */
    public int getTotalSearches() {
        int total = 0;
        try {
            if (popularSearches != null) {
                for (SearchSuggestion suggestion : popularSearches) {
                    if (suggestion != null) {
                        total += suggestion.frequency;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total searches: " + e.getMessage());
        }
        return total;
    }

    /**
     * Get most popular search query
     */
    public String getMostPopularSearch() {
        try {
            if (popularSearches == null || popularSearches.isEmpty()) return null;
            SearchSuggestion mostPopular = popularSearches.get(0);
            return mostPopular != null ? mostPopular.query : null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting most popular search: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a query exists in history
     */
    public boolean isInHistory(String query) {
        try {
            return searchHistory != null && query != null && searchHistory.contains(query);
        } catch (Exception e) {
            Log.e(TAG, "Error checking history: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get recent searches (last N items)
     */
    public List<String> getRecentSearches(int count) {
        try {
            if (searchHistory == null || searchHistory.isEmpty()) {
                return new ArrayList<>();
            }
            int limit = Math.min(count, searchHistory.size());
            return new ArrayList<>(searchHistory.subList(0, limit));
        } catch (Exception e) {
            Log.e(TAG, "Error getting recent searches: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Refresh categories from Firebase
     */
    public void refreshCategories() {
        loadSearchCategories(categoryLoadCallback);
    }

    /**
     * Check if the manager is properly initialized
     */
    public boolean isInitialized() {
        return searchHistory != null && popularSearches != null;
    }
}