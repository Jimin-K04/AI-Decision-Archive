package com.example.termproject.timeCapsule

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import android.widget.ImageView
import java.io.File
import com.example.termproject.R

class TimeCapsuleAdapter(
    private val onSaveReview: (TimeCapsuleItem, ReviewInput) -> Unit,
    private val onAiAnalyze: (TimeCapsuleItem) -> Unit
) : RecyclerView.Adapter<TimeCapsuleAdapter.TimeCapsuleViewHolder>() {

    private val items = mutableListOf<TimeCapsuleItem>()
    private var expandedId: String? = null

    private val satisfactionMap = mutableMapOf<String, Int>()
    private val resultMap = mutableMapOf<String, String>()
    private val regretMap = mutableMapOf<String, String>()
    private val retryMap = mutableMapOf<String, String>()

    fun submitList(newItems: List<TimeCapsuleItem>) {
        items.clear()
        items.addAll(newItems)
        expandedId = newItems.firstOrNull()?.id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeCapsuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.section2_item_time_capsule, parent, false)

        return TimeCapsuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeCapsuleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class TimeCapsuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rootCard: LinearLayout = itemView.findViewById(R.id.rootCard)
        private val iconBox: View = itemView.findViewById(R.id.iconBox)

        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvReviewStatus: TextView = itemView.findViewById(R.id.tvReviewStatus)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvOriginalMemo: TextView = itemView.findViewById(R.id.tvOriginalMemo)
        private val tvArrow: TextView = itemView.findViewById(R.id.tvArrow)

        private val expandArea: LinearLayout = itemView.findViewById(R.id.expandArea)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)

        private val previousReviewBox: LinearLayout = itemView.findViewById(R.id.previousReviewBox)
        private val tvPreviousReview: TextView = itemView.findViewById(R.id.tvPreviousReview)

        private val tvSatisfaction: TextView = itemView.findViewById(R.id.tvSatisfaction)
        private val seekSatisfaction: SeekBar = itemView.findViewById(R.id.seekSatisfaction)

        private val btnSuccess: TextView = itemView.findViewById(R.id.btnSuccess)
        private val btnSoso: TextView = itemView.findViewById(R.id.btnSoso)
        private val btnFail: TextView = itemView.findViewById(R.id.btnFail)

        private val btnRegretNone: TextView = itemView.findViewById(R.id.btnRegretNone)
        private val btnRegretLittle: TextView = itemView.findViewById(R.id.btnRegretLittle)
        private val btnRegretMany: TextView = itemView.findViewById(R.id.btnRegretMany)

        private val retryBox: LinearLayout = itemView.findViewById(R.id.retryBox)
        private val btnSameChoice: TextView = itemView.findViewById(R.id.btnSameChoice)
        private val btnOtherChoice: TextView = itemView.findViewById(R.id.btnOtherChoice)

        private val editMemo: EditText = itemView.findViewById(R.id.editMemo)
        private val btnSaveReview: TextView = itemView.findViewById(R.id.btnSaveReview)
        private val btnAiAnalyze: TextView = itemView.findViewById(R.id.btnAiAnalyze)

        private val itemImageView: ImageView = itemView.findViewById(R.id.itemImageView)
        private val defaultFlowerText: TextView = itemView.findViewById(R.id.defaultFlowerText)

        fun bind(item: TimeCapsuleItem) {
            applyBaseStyles()

            bindTextOrGone(tvCategory, item.category)
            bindTextOrGone(tvDate, item.dateText)

            val displayTitle = if (item.title.isNotBlank()) {
                item.title
            } else {
                item.category
            }

            tvTitle.text = displayTitle
            tvTitle.visibility = if (displayTitle.isBlank()) View.GONE else View.VISIBLE

            if (item.selectedOption.isNotBlank()) {
                tvOriginalMemo.visibility = View.VISIBLE
                tvOriginalMemo.text = "→ ${item.selectedOption}"
            } else {
                tvOriginalMemo.visibility = View.VISIBLE
                tvOriginalMemo.text = "→ 감정점수 ${item.emotionScore} · ${item.discomfort}"
            }

            val reasonText = buildString {
                if (item.reason.isNotBlank()) {
                    append("당시 이유: ${item.reason}")
                }

                if (
                    item.locationText.isNotBlank() &&
                    item.locationText != "위치 정보 없음" &&
                    item.locationText != "주소 알 수 없음"
                ) {
                    if (isNotEmpty()) append("\n")
                    append("당시 위치: ${item.locationText}")
                }

                if (
                    item.weather.isNotBlank() ||
                    item.temperature != 0.0 ||
                    item.humidity != 0
                ) {
                    if (isNotEmpty()) append("\n")
                    append("당시 상태: ${item.weather} · ${item.temperature}℃ · 습도 ${item.humidity}%")
                }
            }

            if (reasonText.isBlank()) {
                tvReason.visibility = View.GONE
            } else {
                tvReason.visibility = View.VISIBLE
                tvReason.text = reasonText
            }
            if (item.previousReviewText.isBlank()) {
                previousReviewBox.visibility = View.GONE
            } else {
                previousReviewBox.visibility = View.VISIBLE
                tvPreviousReview.text = item.previousReviewText
            }

            if (item.reviewCompleted) {
                tvReviewStatus.text = "⊙ 회고완료"
                tvReviewStatus.setTextColor(Color.parseColor("#30A878"))
                tvReviewStatus.background = roundedBg("#DDF7EC", 7)
            } else {
                tvReviewStatus.text = "🔔 회고하기"
                tvReviewStatus.setTextColor(Color.parseColor("#E05A3C"))
                tvReviewStatus.background = roundedBg("#FFE5DF", 7)
            }

            if (!item.imagePath.isNullOrBlank() && File(item.imagePath).exists()) {
                itemImageView.setImageURI(Uri.fromFile(File(item.imagePath)))
                itemImageView.visibility = View.VISIBLE
                defaultFlowerText.visibility = View.GONE
            } else {
                itemImageView.setImageDrawable(null)
                itemImageView.visibility = View.GONE
                defaultFlowerText.visibility = View.VISIBLE
            }

            val isExpanded = expandedId == item.id
            expandArea.visibility = if (isExpanded) View.VISIBLE else View.GONE
            tvArrow.text = if (isExpanded) "⌃" else "›"

            rootCard.setOnClickListener {
                expandedId = if (expandedId == item.id) null else item.id
                notifyDataSetChanged()
            }

            val satisfaction = satisfactionMap[item.id] ?: 3
            val result = resultMap[item.id] ?: "애매"
            val regret = regretMap[item.id] ?: "없음"
            val retry = retryMap[item.id] ?: "같은 선택"

            updateSatisfaction(item.id, satisfaction)
            updateResultButtons(result)
            updateRegretButtons(regret)
            updateRetryButtons(retry)

            seekSatisfaction.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val value = progress + 1
                    satisfactionMap[item.id] = value
                    tvSatisfaction.text = "만족도 ${value}/5"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            btnSuccess.setOnClickListener {
                resultMap[item.id] = "성공"
                updateResultButtons("성공")
            }

            btnSoso.setOnClickListener {
                resultMap[item.id] = "애매"
                updateResultButtons("애매")
            }

            btnFail.setOnClickListener {
                resultMap[item.id] = "실패"
                updateResultButtons("실패")
            }

            btnRegretNone.setOnClickListener {
                regretMap[item.id] = "없음"
                updateRegretButtons("없음")
            }

            btnRegretLittle.setOnClickListener {
                regretMap[item.id] = "조금"
                updateRegretButtons("조금")
            }

            btnRegretMany.setOnClickListener {
                regretMap[item.id] = "많이"
                updateRegretButtons("많이")
            }

            btnSameChoice.setOnClickListener {
                retryMap[item.id] = "같은 선택"
                updateRetryButtons("같은 선택")
            }

            btnOtherChoice.setOnClickListener {
                retryMap[item.id] = "다른 선택"
                updateRetryButtons("다른 선택")
            }

            btnSaveReview.setOnClickListener {
                val input = ReviewInput(
                    satisfaction = satisfactionMap[item.id] ?: 3,
                    result = resultMap[item.id] ?: "애매",
                    regret = regretMap[item.id] ?: "없음",
                    retryChoice = retryMap[item.id] ?: "같은 선택",
                    memo = editMemo.text.toString()
                )

                onSaveReview(item, input)
            }

            btnAiAnalyze.setOnClickListener {
                onAiAnalyze(item)
            }
        }

        private fun bindTextOrGone(view: TextView, value: String) {
            if (value.isBlank()) {
                view.visibility = View.GONE
            } else {
                view.visibility = View.VISIBLE
                view.text = value
            }
        }

        private fun updateSatisfaction(itemId: String, value: Int) {
            tvSatisfaction.text = "만족도 ${value}/5"
            seekSatisfaction.progress = value - 1
        }

        private fun updateResultButtons(selected: String) {
            setSelectedButton(btnSuccess, selected == "성공")
            setSelectedButton(btnSoso, selected == "애매")
            setSelectedButton(btnFail, selected == "실패")
        }

        private fun updateRegretButtons(selected: String) {
            setSelectedButton(btnRegretNone, selected == "없음")
            setSelectedButton(btnRegretLittle, selected == "조금")
            setSelectedButton(btnRegretMany, selected == "많이")
        }

        private fun updateRetryButtons(selected: String) {
            setSmallSelectedButton(btnSameChoice, selected == "같은 선택")
            setSmallSelectedButton(btnOtherChoice, selected == "다른 선택")
        }

        private fun setSelectedButton(view: TextView, selected: Boolean) {
            if (selected) {
                view.background = gradientBg("#E05A3C", "#D94D31", 24)
                view.setTextColor(Color.WHITE)
            } else {
                view.background = roundedBg("#FFFFFF", 24, "#E8D8CA", 1)
                view.setTextColor(Color.parseColor("#2B1712"))
            }
        }

        private fun setSmallSelectedButton(view: TextView, selected: Boolean) {
            if (selected) {
                view.background = gradientBg("#E05A3C", "#D94D31", 24)
                view.setTextColor(Color.WHITE)
            } else {
                view.background = roundedBg("#F5F2EF", 24)
                view.setTextColor(Color.parseColor("#7E6A5D"))
            }
        }

        private fun applyBaseStyles() {
            rootCard.background = roundedBg("#FFFFFC", 24, "#E8D8CA", 1)
            iconBox.background = roundedBg("#FFE3B8", 17)
            tvCategory.background = roundedBg("#F1E6D8", 7)

            previousReviewBox.background = roundedBg("#FFFFFF", 18, "#E8D8CA", 1)
            retryBox.background = roundedBg("#FFFFFF", 18, "#E8D8CA", 1)
            editMemo.background = roundedBg("#FFFFFF", 18, "#E8D8CA", 1)

            btnSaveReview.background = gradientBg("#E05A3C", "#D94D31", 27)
            btnAiAnalyze.background = roundedBg("#FFFFFF", 26, "#E05A3C", 2)
        }

        private fun dp(value: Int): Int {
            return (value * itemView.resources.displayMetrics.density).toInt()
        }

        private fun roundedBg(
            color: String,
            radius: Int,
            strokeColor: String? = null,
            strokeWidth: Int = 0
        ): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor(color))
                cornerRadius = dp(radius).toFloat()

                if (strokeColor != null && strokeWidth > 0) {
                    setStroke(dp(strokeWidth), Color.parseColor(strokeColor))
                }
            }
        }

        private fun gradientBg(
            startColor: String,
            endColor: String,
            radius: Int
        ): GradientDrawable {
            return GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.parseColor(startColor),
                    Color.parseColor(endColor)
                )
            ).apply {
                cornerRadius = dp(radius).toFloat()
            }
        }
    }
}