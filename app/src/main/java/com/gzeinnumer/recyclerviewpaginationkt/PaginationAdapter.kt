package com.gzeinnumer.recyclerviewpaginationkt

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.gzeinnumer.recyclerviewpaginationkt.models.Result
import com.gzeinnumer.recyclerviewpaginationkt.utils.GlideApp
import com.gzeinnumer.recyclerviewpaginationkt.utils.GlideRequest
import com.gzeinnumer.recyclerviewpaginationkt.utils.PaginationAdapterCallback
import java.util.*


class PaginationAdapter internal constructor(private val context: Context, private val mCallback: MainActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        // View Types
        private const val ITEM = 0
        private const val LOADING = 1
        private const val HERO = 2
        private val BASE_URL_IMG: String? = "https://image.tmdb.org/t/p/w200"
    }

    var movieResults: MutableList<Result> = ArrayList<Result>()
    var isLoadingAdded = false
    var retryPageLoad = false

//    private var mCallback: PaginationAdapterCallback? = null
    var errorMsg: String? = null


    init {
//        mCallback = context as PaginationAdapterCallback
        movieResults = ArrayList()
    }

    var movies: MutableList<Result>
        get() = movieResults
        set(movieResults) {
            this.movieResults = movieResults
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder: RecyclerView.ViewHolder? = null
        when (viewType) {
            ITEM -> {
                val viewItem = inflater.inflate(R.layout.item_list, parent, false)
                return MovieVH(viewItem)
            }
            LOADING -> {
                val viewLoading = inflater.inflate(R.layout.item_progress, parent, false)
                return LoadingVH(viewLoading)
            }
            HERO -> {
                val viewHero = inflater.inflate(R.layout.item_hero, parent, false)
                return HeroVH(viewHero)
            }
        }
        return viewHolder!!
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val result: Result = movieResults[position] // Movie
        when (getItemViewType(position)) {
            HERO -> {
                val heroVh = holder as HeroVH
                heroVh.mMovieTitle.text = result.title
                heroVh.mYear.text = formatYearLabel(result)
                heroVh.mMovieDesc.text = result.overview
                result.backdropPath?.let {
                    loadImage(it).into(heroVh.mPosterImg)
                }
            }
            ITEM -> {
                val movieVH = holder as MovieVH
                movieVH.mMovieTitle.text = result.title
                movieVH.mYear.text = formatYearLabel(result)
                movieVH.mMovieDesc.text = result.overview

                // load movie thumbnail
                result.posterPath?.let {
                    loadImage(it)
                        .listener(object : RequestListener<Drawable?> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable?>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                movieVH.mProgress.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable?>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                movieVH.mProgress.visibility = View.GONE
                                return false
                            }
                        })
                        .into(movieVH.mPosterImg)
                }
            }
            LOADING -> {
                val loadingVH = holder as LoadingVH
                if (retryPageLoad) {
                    loadingVH.mErrorLayout.visibility = View.VISIBLE
                    loadingVH.mProgressBar.visibility = View.GONE
                    loadingVH.mErrorTxt.text =
                        if (errorMsg != null) errorMsg else context.getString(R.string.error_msg_unknown)
                } else {
                    loadingVH.mErrorLayout.visibility = View.GONE
                    loadingVH.mProgressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return movieResults.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            HERO
        } else {
            if (position == movieResults.size - 1 && isLoadingAdded) LOADING else ITEM
        }
    }

    fun formatYearLabel(result: Result): String {
        return (result.releaseDate?.substring(0, 4) // we want the year only
                + " | " + result.originalLanguage?.toUpperCase())
    }

    fun loadImage(posterPath: String): GlideRequest<Drawable> {
        return GlideApp
            .with(context)
            .load(BASE_URL_IMG + posterPath)
            .centerCrop()
    }

    fun add(r: Result) {
        movieResults.add(r)
        notifyItemInserted(movieResults.size - 1)
    }

    fun addAll(moveResults: MutableList<Result>) {
        for (result in moveResults) {
            add(result)
        }
    }

    fun remove(r: Result) {
        val position = movieResults.indexOf(r)
        if (position > -1) {
            movieResults.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clear() {
        isLoadingAdded = false
        while (itemCount > 0) {
            getItem(0)?.let { remove(it) }
        }
    }

    val isEmpty: Boolean
        get() = itemCount == 0

    fun addLoadingFooter() {
        isLoadingAdded = true
        add(Result())
    }

    fun removeLoadingFooter() {
        isLoadingAdded = false
        val position = movieResults.size - 1
        val result: Result? = getItem(position)
        if (result != null) {
            movieResults.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getItem(position: Int): Result? {
        return movieResults[position]
    }

    fun showRetry(show: Boolean, errorMsg: String?) {
        retryPageLoad = show
        notifyItemChanged(movieResults.size - 1)
        if (errorMsg != null) this.errorMsg = errorMsg
    }

    inner class HeroVH(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mMovieTitle: TextView = itemView.findViewById(R.id.movie_title)
        val mMovieDesc: TextView = itemView.findViewById(R.id.movie_desc)
        val mYear: TextView = itemView.findViewById(R.id.movie_year)
        val mPosterImg: ImageView = itemView.findViewById(R.id.movie_poster)
    }

    inner class MovieVH(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val mMovieTitle: TextView = itemView.findViewById(R.id.movie_title)
        val mMovieDesc: TextView = itemView.findViewById(R.id.movie_desc)
        val mYear : TextView = itemView.findViewById(R.id.movie_year)
        val mPosterImg: ImageView = itemView.findViewById(R.id.movie_poster)
        val mProgress: ProgressBar = itemView.findViewById(R.id.movie_progress)
    }

    inner class LoadingVH(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val mProgressBar: ProgressBar = itemView.findViewById(R.id.loadmore_progress)
        val mRetryBtn: ImageButton = itemView.findViewById(R.id.loadmore_retry)
        val mErrorTxt: TextView = itemView.findViewById(R.id.loadmore_errortxt)
        val mErrorLayout: LinearLayout = itemView.findViewById(R.id.loadmore_errorlayout)

        init {
            mRetryBtn.setOnClickListener(this)
            mErrorLayout.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            when (view.id) {
                R.id.loadmore_retry, R.id.loadmore_errorlayout -> {
                    showRetry(false, null)
                    mCallback.retryPageLoad()
                }
            }
        }
    }
}