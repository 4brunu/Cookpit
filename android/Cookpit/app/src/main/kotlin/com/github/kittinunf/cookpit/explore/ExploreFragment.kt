package com.github.kittinunf.cookpit.explore

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.github.kittinunf.cookpit.BaseFragment
import com.github.kittinunf.cookpit.R
import com.github.kittinunf.cookpit.photo.PhotoViewActivity
import com.github.kittinunf.cookpit.util.rx_staggeredLoadMore
import com.github.kittinunf.cookpit.util.setImage
import com.github.kittinunf.reactiveandroid.rx.addTo
import com.github.kittinunf.reactiveandroid.rx.bindTo
import com.github.kittinunf.reactiveandroid.support.v4.widget.rx_refresh
import com.github.kittinunf.reactiveandroid.support.v4.widget.rx_refreshing
import com.github.kittinunf.reactiveandroid.support.v7.widget.rx_itemsWith
import kotlinx.android.synthetic.main.fragment_explore.*
import kotlinx.android.synthetic.main.recycler_item_explore.view.*
import rx.Observable

class ExploreFragment : BaseFragment() {

    override val resourceId: Int = R.layout.fragment_explore

    private val screenSize: Point by lazy {
        val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        size
    }

    private val viewModel = ExploreViewModel()

    override fun setUp() {
        viewModel.requestForPage(1)
    }

    override fun setUp(view: View) {
        exploreRecyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        viewModel.loadings.bindTo(exploreSwipeRefreshLayout.rx_refreshing).addTo(subscriptions)

        exploreSwipeRefreshLayout.rx_refresh().subscribe {
            viewModel.reset()
            viewModel.requestForPage(1)
        }.addTo(subscriptions)

        exploreRecyclerView.rx_itemsWith(viewModel.items, { viewGroup, index ->
            val itemView = LayoutInflater.from(viewGroup?.context).inflate(R.layout.recycler_item_explore, viewGroup, false)
            val viewHolder = ExploreViewHolder(itemView)
            viewHolder.onClick = { selectedIndex ->
                viewModel[selectedIndex]?.let {
                    val intent = Intent(activity, PhotoViewActivity::class.java)
                    intent.putExtra(PhotoViewActivity.PHOTO_ID_EXTRA, it.id)
                    this@ExploreFragment.startActivity(intent)
                }
            }
            viewHolder
        }, { viewHolder, index, item ->
            viewHolder.cardView.preventCornerOverlap = false
            viewHolder.backgroundImageView.setImage(item.imageUrl, screenSize.x / 2, 600)
            viewHolder.titleTextView.text = item.title
        }).addTo(subscriptions)

        Observable.combineLatest(exploreRecyclerView.rx_staggeredLoadMore(), viewModel.loadings) { more, loading ->
            more and !loading
        }.filter { it }.subscribe {
            viewModel.requestForNextPage()
        }
    }

    class ExploreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView by lazy { view.exploreCardView }
        val backgroundImageView by lazy { view.exploreBackgroundImageView }
        val titleTextView by lazy { view.exploreTitleTextView }

        var onClick: ((Int) -> Unit)? = null

        init {
           view.setOnClickListener { onClick?.invoke(layoutPosition) }
        }
    }

}