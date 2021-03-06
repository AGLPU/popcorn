package com.ang.mov3.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ang.mov3.R;
import com.ang.mov3.adapters.TVShowBriefsSmallAdapter;
import com.ang.mov3.network.ApiClient;
import com.ang.mov3.network.ApiInterface;
import com.ang.mov3.network.tvshows.AiringTodayTVShowsResponse;
import com.ang.mov3.network.tvshows.OnTheAirTVShowsResponse;
import com.ang.mov3.network.tvshows.PopularTVShowsResponse;
import com.ang.mov3.network.tvshows.TVShowBrief;
import com.ang.mov3.network.tvshows.TopRatedTVShowsResponse;
import com.ang.mov3.utils.Constants;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;

//import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewAllTVShowsActivity extends AppCompatActivity {

//    private SmoothProgressBar mSmoothProgressBar;

    private RecyclerView mRecyclerView;
    private List<TVShowBrief> mTVShows;
    private TVShowBriefsSmallAdapter mTVShowsAdapter;

    private int mTVShowType;

    AdView mAdView;
    private boolean pagesOver = false;
    private int presentPage = 1;
    private boolean loading = true;
    private int previousTotal = 0;
    private int visibleThreshold = 5;

    InterstitialAd interstitialAd;
    private Call<AiringTodayTVShowsResponse> mAiringTodayTVShowsCall;
    private Call<OnTheAirTVShowsResponse> mOnTheAirTVShowsCall;
    private Call<PopularTVShowsResponse> mPopularTVShowsCall;
    private Call<TopRatedTVShowsResponse> mTopRatedTVShowsCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_tvshows);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent receivedIntent = getIntent();
        mTVShowType = receivedIntent.getIntExtra(Constants.VIEW_ALL_TV_SHOWS_TYPE, -1);
        mAdView = (AdView)findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        MobileAds.initialize(this,
                "ca-app-pub-3136435737091654~7695867822");
        interstitialAd = new InterstitialAd(ViewAllTVShowsActivity.this);

        interstitialAd.setAdUnitId("ca-app-pub-3136435737091654/7504296138");
        // Create ad request.

        interstitialAd.loadAd(adRequest);

        if (mTVShowType == -1) finish();

        switch (mTVShowType) {
            case Constants.AIRING_TODAY_TV_SHOWS_TYPE:
                setTitle(R.string.airing_today_tv_shows);
                break;
            case Constants.ON_THE_AIR_TV_SHOWS_TYPE:
                setTitle(R.string.on_the_air_tv_shows);
                break;
            case Constants.POPULAR_TV_SHOWS_TYPE:
                setTitle(R.string.popular_tv_shows);
                break;
            case Constants.TOP_RATED_TV_SHOWS_TYPE:
                setTitle(R.string.top_rated_tv_shows);
                break;
        }

//        mSmoothProgressBar = (SmoothProgressBar) findViewById(R.id.smooth_progress_bar);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_view_all);
        mTVShows = new ArrayList<>();
        mTVShowsAdapter = new TVShowBriefsSmallAdapter(ViewAllTVShowsActivity.this, mTVShows);
        mRecyclerView.setAdapter(mTVShowsAdapter);
        final GridLayoutManager gridLayoutManager = new GridLayoutManager(ViewAllTVShowsActivity.this, 3);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int firstVisibleItem = gridLayoutManager.findFirstVisibleItemPosition();

                if (loading) {
                    if (totalItemCount > previousTotal) {
                        loading = false;
                        previousTotal = totalItemCount;
                    }
                }
                if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                    loadTVShows(mTVShowType);
                    loading = true;
                }

            }
        });

        loadTVShows(mTVShowType);

    }

    @Override
    protected void onStart() {
        super.onStart();

        mTVShowsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (interstitialAd.isLoaded()) {
            interstitialAd.show();
            interstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    finish();
                }
            });
        }else{
            super.onBackPressed();
        }

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAiringTodayTVShowsCall != null) mAiringTodayTVShowsCall.cancel();
        if (mOnTheAirTVShowsCall != null) mOnTheAirTVShowsCall.cancel();
        if (mPopularTVShowsCall != null) mPopularTVShowsCall.cancel();
        if (mTopRatedTVShowsCall != null) mTopRatedTVShowsCall.cancel();
    }

    private void loadTVShows(int tvShowType) {
        if (pagesOver) return;

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
//        mSmoothProgressBar.progressiveStart();

        switch (tvShowType) {
            case Constants.AIRING_TODAY_TV_SHOWS_TYPE:
                mAiringTodayTVShowsCall = apiService.getAiringTodayTVShows(getResources().getString(R.string.MOVIE_DB_API_KEY), presentPage);
                mAiringTodayTVShowsCall.enqueue(new Callback<AiringTodayTVShowsResponse>() {
                    @Override
                    public void onResponse(Call<AiringTodayTVShowsResponse> call, Response<AiringTodayTVShowsResponse> response) {
                        if (!response.isSuccessful()) {
                            mAiringTodayTVShowsCall = call.clone();
                            mAiringTodayTVShowsCall.enqueue(this);
                            return;
                        }

                        if (response.body() == null) return;
                        if (response.body().getResults() == null) return;

//                        mSmoothProgressBar.progressiveStop();
                        for (TVShowBrief tvShowBrief : response.body().getResults()) {
                            if (tvShowBrief != null && tvShowBrief.getName() != null && tvShowBrief.getPosterPath() != null)
                                mTVShows.add(tvShowBrief);
                        }
                        mTVShowsAdapter.notifyDataSetChanged();
                        if (response.body().getPage() == response.body().getTotalPages())
                            pagesOver = true;
                        else
                            presentPage++;
                    }

                    @Override
                    public void onFailure(Call<AiringTodayTVShowsResponse> call, Throwable t) {

                    }
                });
                break;
            case Constants.ON_THE_AIR_TV_SHOWS_TYPE:
                mOnTheAirTVShowsCall = apiService.getOnTheAirTVShows(getResources().getString(R.string.MOVIE_DB_API_KEY), presentPage);
                mOnTheAirTVShowsCall.enqueue(new Callback<OnTheAirTVShowsResponse>() {
                    @Override
                    public void onResponse(Call<OnTheAirTVShowsResponse> call, Response<OnTheAirTVShowsResponse> response) {
                        if (!response.isSuccessful()) {
                            mOnTheAirTVShowsCall = call.clone();
                            mOnTheAirTVShowsCall.enqueue(this);
                            return;
                        }

                        if (response.body() == null) return;
                        if (response.body().getResults() == null) return;

//                        mSmoothProgressBar.progressiveStop();
                        for (TVShowBrief tvShowBrief : response.body().getResults()) {
                            if (tvShowBrief != null && tvShowBrief.getName() != null && tvShowBrief.getPosterPath() != null)
                                mTVShows.add(tvShowBrief);
                        }
                        mTVShowsAdapter.notifyDataSetChanged();
                        if (response.body().getPage() == response.body().getTotalPages())
                            pagesOver = true;
                        else
                            presentPage++;
                    }

                    @Override
                    public void onFailure(Call<OnTheAirTVShowsResponse> call, Throwable t) {

                    }
                });
                break;
            case Constants.POPULAR_TV_SHOWS_TYPE:
                mPopularTVShowsCall = apiService.getPopularTVShows(getResources().getString(R.string.MOVIE_DB_API_KEY), presentPage);
                mPopularTVShowsCall.enqueue(new Callback<PopularTVShowsResponse>() {
                    @Override
                    public void onResponse(Call<PopularTVShowsResponse> call, Response<PopularTVShowsResponse> response) {
                        if (!response.isSuccessful()) {
                            mPopularTVShowsCall = call.clone();
                            mPopularTVShowsCall.enqueue(this);
                            return;
                        }

                        if (response.body() == null) return;
                        if (response.body().getResults() == null) return;

//                        mSmoothProgressBar.progressiveStop();
                        for (TVShowBrief tvShowBrief : response.body().getResults()) {
                            if (tvShowBrief != null && tvShowBrief.getName() != null && tvShowBrief.getPosterPath() != null)
                                mTVShows.add(tvShowBrief);
                        }
                        mTVShowsAdapter.notifyDataSetChanged();
                        if (response.body().getPage() == response.body().getTotalPages())
                            pagesOver = true;
                        else
                            presentPage++;
                    }

                    @Override
                    public void onFailure(Call<PopularTVShowsResponse> call, Throwable t) {

                    }
                });
                break;
            case Constants.TOP_RATED_TV_SHOWS_TYPE:
                mTopRatedTVShowsCall = apiService.getTopRatedTVShows(getResources().getString(R.string.MOVIE_DB_API_KEY), presentPage);
                mTopRatedTVShowsCall.enqueue(new Callback<TopRatedTVShowsResponse>() {
                    @Override
                    public void onResponse(Call<TopRatedTVShowsResponse> call, Response<TopRatedTVShowsResponse> response) {
                        if (!response.isSuccessful()) {
                            mTopRatedTVShowsCall = call.clone();
                            mTopRatedTVShowsCall.enqueue(this);
                            return;
                        }

                        if (response.body() == null) return;
                        if (response.body().getResults() == null) return;

//                        mSmoothProgressBar.progressiveStop();
                        for (TVShowBrief tvShowBrief : response.body().getResults()) {
                            if (tvShowBrief != null && tvShowBrief.getName() != null && tvShowBrief.getPosterPath() != null)
                                mTVShows.add(tvShowBrief);
                        }
                        mTVShowsAdapter.notifyDataSetChanged();
                        if (response.body().getPage() == response.body().getTotalPages())
                            pagesOver = true;
                        else
                            presentPage++;
                    }

                    @Override
                    public void onFailure(Call<TopRatedTVShowsResponse> call, Throwable t) {

                    }
                });
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}
