/*
 * Copyright (C) 2026 The BlissROM Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.bliss.iconpack

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.launcher3.R

/**
 * Adapter for the icon pack picker list. Displays "System Default" as the first item, followed by
 * all installed icon packs. Each card shows the pack's app icon, a row of sample themed icons, and
 * the pack label. The currently active pack is shown with an activated state.
 */
class IconPackPreviewAdapter(
    private val items: List<IconPackInfo>,
    private val activePackage: String?,
    private val isMonoThemeActive: Boolean,
    private val onPackSelected: (IconPackInfo) -> Unit,
) : RecyclerView.Adapter<IconPackPreviewAdapter.ViewHolder>() {

    private var selectedPosition: Int =
        if (isMonoThemeActive) {
            RecyclerView.NO_POSITION
        } else {
            items
                .indexOfFirst { item ->
                    if (activePackage != null) item.packageName == activePackage
                    else item.isSystemDefault
                }
                .coerceAtLeast(0)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.bliss_icon_pack_preview_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val isSelected = position == selectedPosition

        holder.icon.setImageDrawable(item.icon)
        holder.label.text = item.label
        holder.itemView.isActivated = isSelected

        bindPreviewIcons(holder, item.previewIcons)

        val context = holder.itemView.context
        val baseDescription =
            context.getString(R.string.bliss_icon_pack_content_description, item.label)
        holder.itemView.contentDescription =
            if (isSelected) {
                "$baseDescription, ${context.getString(R.string.bliss_icon_pack_current_label)}"
            } else {
                baseDescription
            }

        holder.itemView.setOnClickListener {
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            if (adapterPosition == selectedPosition) return@setOnClickListener

            val previousPosition = selectedPosition
            selectedPosition = adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onPackSelected(item)
        }
    }

    private fun bindPreviewIcons(holder: ViewHolder, previewIcons: List<Drawable?>) {
        val hasAnyPreview = previewIcons.any { it != null }
        holder.previewRow.visibility = if (hasAnyPreview) View.VISIBLE else View.GONE

        for (i in holder.sampleIcons.indices) {
            val drawable = previewIcons.getOrNull(i)
            holder.sampleIcons[i].setImageDrawable(drawable)
            holder.sampleIcons[i].visibility = if (drawable != null) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.bliss_icon_pack_preview_icon)
        val label: TextView = itemView.findViewById(R.id.bliss_icon_pack_label)
        val previewRow: LinearLayout = itemView.findViewById(R.id.bliss_icon_pack_preview_row)
        val sampleIcons: List<ImageView> =
            listOf(
                itemView.findViewById(R.id.bliss_icon_pack_sample_0),
                itemView.findViewById(R.id.bliss_icon_pack_sample_1),
                itemView.findViewById(R.id.bliss_icon_pack_sample_2),
                itemView.findViewById(R.id.bliss_icon_pack_sample_3),
            )
    }
}
