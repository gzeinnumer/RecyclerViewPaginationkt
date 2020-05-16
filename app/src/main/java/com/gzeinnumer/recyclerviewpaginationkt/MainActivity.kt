package com.gzeinnumer.recyclerviewpaginationkt

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.gzeinnumer.recyclerviewpaginationkt.api.MovieApi
import com.gzeinnumer.recyclerviewpaginationkt.api.MovieService
import com.gzeinnumer.recyclerviewpaginationkt.models.TopRatedMovies
import com.gzeinnumer.recyclerviewpaginationkt.utils.PaginationScrollListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeoutException
import com.gzeinnumer.recyclerviewpaginationkt.models.Result


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    lateinit var adapter: PaginationAdapter
    lateinit var linearLayoutManager: LinearLayoutManager

    lateinit var rv: RecyclerView
    lateinit var progressBar: ProgressBar
    lateinit var errorLayout: LinearLayout
    lateinit var btnRetry: Button
    lateinit var txtError: TextView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val PAGE_START = 1

    private var isLoading = false
    private var isLastPage = false

    // limiting to 5 for this tutorial, since total pages in actual API is very large. Feel free to modify.
    private val TOTAL_PAGES = 5
    private var currentPage = PAGE_START

    lateinit var movieService: MovieService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()

        initRecyclerView()

        initDataForRecyclerView()
    }

    private fun initView() {
        rv = findViewById(R.id.main_recycler)
        progressBar = findViewById(R.id.main_progress)
        errorLayout = findViewById(R.id.error_layout)
        btnRetry = findViewById(R.id.error_btn_retry)
        txtError = findViewById(R.id.error_txt_cause)
        swipeRefreshLayout = findViewById(R.id.main_swiperefresh)
    }

    private fun initRecyclerView() {
        adapter = PaginationAdapter(this, this@MainActivity)
        linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rv.layoutManager = linearLayoutManager
        rv.itemAnimator = DefaultItemAnimator()
        rv.adapter = adapter


        rv.addOnScrollListener(object : PaginationScrollListener(linearLayoutManager) {
            override fun loadMoreItems() {
                isLoading = true
                currentPage += 1
                loadNextPage()
            }

            override fun getTotalPageCount(): Int {
                return TOTAL_PAGES
            }

            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }
        })
    }

    private fun initDataForRecyclerView() {
        MovieApi.getClient(this)?.let {
            movieService = it.create(MovieService::class.java)
        }

        loadFirstPage()
        btnRetry.setOnClickListener { view: View? -> loadFirstPage() }
        swipeRefreshLayout.setOnRefreshListener { doRefresh() }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                // Signal SwipeRefreshLayout to start the progress indicator
                swipeRefreshLayout.isRefreshing = true
                doRefresh()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun doRefresh() {
        progressBar.visibility = View.VISIBLE
        if (callTopRatedMoviesApi().isExecuted) callTopRatedMoviesApi().cancel()

        adapter.movies.clear()
        adapter.notifyDataSetChanged()
        loadFirstPage()
        swipeRefreshLayout.isRefreshing = false
    }

    private fun loadFirstPage() {
        Log.d(TAG, "loadFirstPage: ")

        // To ensure list is visible when retry button in error view is clicked
        hideErrorView()
        currentPage = PAGE_START
        callTopRatedMoviesApi().enqueue(object : Callback<TopRatedMovies> {
            override fun onResponse(
                call: Call<TopRatedMovies>,
                response: Response<TopRatedMovies>
            ) {
                hideErrorView()
                val results = fetchResults(response)
                progressBar.visibility = View.GONE
                adapter.addAll(results as MutableList<Result>)
                if (currentPage <= TOTAL_PAGES) {
                    adapter.addLoadingFooter()
                } else {
                    isLastPage = true
                }
            }

            override fun onFailure(
                call: Call<TopRatedMovies>,
                t: Throwable
            ) {
                t.printStackTrace()
                showErrorView(t)
            }
        })
    }

    private fun fetchResults(response: Response<TopRatedMovies>): List<Result> {
        val topRatedMovies: TopRatedMovies? = response.body()
        return topRatedMovies?.results!!
    }

    private fun loadNextPage() {
        Log.d(TAG, "loadNextPage: $currentPage")
        callTopRatedMoviesApi().enqueue(object : Callback<TopRatedMovies> {
            override fun onResponse(
                call: Call<TopRatedMovies>,
                response: Response<TopRatedMovies>
            ) {
                adapter.removeLoadingFooter()
                isLoading = false
                val results: List<Result> = fetchResults(response)
                adapter.addAll(results as MutableList<Result>)
                if (currentPage != TOTAL_PAGES) adapter.addLoadingFooter() else isLastPage =
                    true
            }

            override fun onFailure(
                call: Call<TopRatedMovies>,
                t: Throwable
            ) {
                t.printStackTrace()
                adapter.showRetry(true, fetchErrorMessage(t))
            }
        })
    }

    private fun callTopRatedMoviesApi(): Call<TopRatedMovies> {
        return movieService.getTopRatedMovies(
            getString(R.string.my_api_key),
            "en_US",
            currentPage
        )
    }


    fun retryPageLoad() {
        loadNextPage()
    }

    private fun showErrorView(throwable: Throwable) {
        if (errorLayout.visibility == View.GONE) {
            errorLayout.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            txtError.text = fetchErrorMessage(throwable)
        }
    }

    private fun fetchErrorMessage(throwable: Throwable): String? {
        var errorMsg = resources.getString(R.string.error_msg_unknown)
        if (!isNetworkConnected()) {
            errorMsg = resources.getString(R.string.error_msg_no_internet)
        } else if (throwable is TimeoutException) {
            errorMsg = resources.getString(R.string.error_msg_timeout)
        }
        return errorMsg
    }

    private fun hideErrorView() {
        if (errorLayout.visibility == View.VISIBLE) {
            errorLayout.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null
    }
}
