/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R

/**
 * Adapter for the icon pack detail grid showing all installed apps with their themed icons.
 *
 * Apps with themed icons are shown at full opacity with the themed icon. Apps without a themed
 * icon show their original system icon at reduced opacity to visually indicate they are unthemed.
 */
class IconPackDetailAdapter(
    private val items: List<IconPackCoverageItem>,
) : RecyclerView.Adapter<IconPackDetailAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.bliss_icon_pack_detail_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val hasTheme = item.themedIcon != null

        holder.icon.setImageDrawable(item.themedIcon ?: item.originalIcon)
        holder.icon.alpha = if (hasTheme) THEMED_ALPHA else UNTHEMED_ALPHA
        holder.label.text = item.label
        holder.label.alpha = if (hasTheme) THEMED_ALPHA else UNTHEMED_ALPHA

        holder.itemView.contentDescription = if (hasTheme) {
            holder.itemView.context.getString(
                R.string.bliss_icon_pack_detail_themed_description, item.label
            )
        } else {
            holder.itemView.context.getString(
                R.string.bliss_icon_pack_detail_unthemed_description, item.label
            )
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.bliss_icon_pack_detail_themed_icon)
        val label: TextView = itemView.findViewById(R.id.bliss_icon_pack_detail_app_label)
    }

    companion object {
        private const val THEMED_ALPHA = 1.0f
        private const val UNTHEMED_ALPHA = 0.35f
    }
}
