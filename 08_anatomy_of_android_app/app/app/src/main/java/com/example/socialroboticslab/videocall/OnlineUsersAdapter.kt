package com.example.socialroboticslab.videocall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnlineUsersAdapter(
    private val onUserClick: (String) -> Unit
) : RecyclerView.Adapter<OnlineUsersAdapter.UserViewHolder>() {

    private var users: List<String> = emptyList()

    fun updateUsers(newUsers: List<String>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(user: String) {
            textView.text = user
            textView.textSize = 16f
            textView.setPadding(16, 12, 16, 12)
            itemView.setOnClickListener { onUserClick(user) }
        }
    }
}
