package ru.takeshiko.matuleme.presentation.main.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.takeshiko.matuleme.R
import ru.takeshiko.matuleme.databinding.ItemNotificationBinding
import ru.takeshiko.matuleme.domain.models.database.UserNotification
import java.util.Locale

class NotificationAdapter(
    private var items: List<UserNotification>,
    private val onItemClick: (UserNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserNotification) {
            with (binding) {
                tvTitle.text = item.title
                tvMark.text = if (!item.isRead) tvMark.context.getString(R.string.new_notification) else ""
                tvDescription.text = item.message
                tvDatetime.text = formatInstant(item.createdAt)

                itemView.setOnClickListener { onItemClick(item) }
            }
        }

        private fun formatInstant(instant: Instant): String {
            val now = Clock.System.now()
            val duration = now - instant

            with (binding.tvDatetime) {
                return when {
                    duration.inWholeSeconds < 60 -> "${duration.inWholeSeconds} ${context.getString(R.string.seconds_ago)}"
                    duration.inWholeMinutes < 60 -> "${duration.inWholeMinutes} ${context.getString(R.string.minutes_ago)}"
                    duration.inWholeHours < 24 -> "${duration.inWholeHours} ${context.getString(R.string.hours_ago)}"
                    duration.inWholeDays < 7 -> "${duration.inWholeDays} ${context.getString(R.string.days_ago)}"
                    else -> {
                        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                        String.format(
                            Locale.getDefault(),
                            "%02d.%02d.%04d, %02d:%02d",
                            localDateTime.dayOfMonth,
                            localDateTime.monthNumber,
                            localDateTime.year,
                            localDateTime.hour,
                            localDateTime.minute
                        )
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<UserNotification>) {
        val diffCallback = NotificationDiffCallback(items, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = newList
        diffResult.dispatchUpdatesTo(this)
    }

    class NotificationDiffCallback(
        private val oldList: List<UserNotification>,
        private val newList: List<UserNotification>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}