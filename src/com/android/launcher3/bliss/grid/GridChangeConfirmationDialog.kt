/*
 * Copyright (C) 2026 The BlissROMs Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.bliss.grid

import android.app.AlertDialog
import android.content.Context
import com.android.launcher3.R

/**
 * Shows a confirmation dialog before applying a grid size change,
 * warning the user that their home screen layout may be rearranged.
 */
object GridChangeConfirmationDialog {

    fun show(
        context: Context,
        numColumns: Int,
        numRows: Int,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(R.string.grid_change_dialog_title)
            .setMessage(
                context.getString(
                    R.string.grid_change_dialog_message, numColumns, numRows
                )
            )
            .setPositiveButton(R.string.grid_change_dialog_apply) { _, _ ->
                onConfirm()
            }
            .setNegativeButton(R.string.grid_change_dialog_cancel, null)
            .show()
    }
}
