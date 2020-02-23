/*
 * Copyright 2012 GitHub Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.mobile.ui.issue

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import com.github.kevinsawicki.wishlist.MultiTypeAdapter
import com.github.mobile.R
import com.github.mobile.api.model.TimelineEvent
import com.github.mobile.api.model.User
import com.github.mobile.ui.ReactionsView
import com.github.mobile.ui.user.UserViewActivity
import com.github.mobile.util.AvatarLoader
import com.github.mobile.util.HttpImageGetter
import com.github.mobile.util.TimeUtils
import com.github.mobile.util.TypefaceUtils
import java.util.*

/**
 * Adapter for a list of [TimelineEvent] objects
 */
class EventListAdapter(
    activity: Activity, avatars: AvatarLoader,
    imageGetter: HttpImageGetter, issueFragment: IssueFragment,
    isCollaborator: Boolean, loggedUser: String
) : MultiTypeAdapter(activity.layoutInflater) {
    private val context: Context
    private val resources: Resources
    private val avatars: AvatarLoader
    private val imageGetter: HttpImageGetter
    private val issueFragment: IssueFragment
    private val user: String
    private val isCollaborator: Boolean
    override fun update(position: Int, obj: Any, type: Int) {
        when (type) {
            VIEW_COMMENT -> updateComment(obj as TimelineEvent)
            VIEW_EVENT -> updateEvent(obj as TimelineEvent)
        }
    }

    private fun updateEvent(event: TimelineEvent) {
        val eventString = event.event
        val actor: User?
        actor = when (eventString) {
            TimelineEvent.EVENT_ASSIGNED, TimelineEvent.EVENT_UNASSIGNED -> event.assignee
            TimelineEvent.EVENT_REVIEWED -> event.user
            else -> event.actor
        }
        var message: String? =
            String.format("<b>%s</b> ", if (actor == null) "ghost" else actor.login)
        if (actor != null) {
            setGone(2, false)
            avatars.bind(imageView(2), actor)
        } else {
            setGone(2, true)
        }
        when (eventString) {
            TimelineEvent.EVENT_ASSIGNED -> {
                var assignedTextResource = R.string.issue_event_label_assigned
                if (event.actor.id == event.assignee.id) {
                    assignedTextResource = R.string.issue_event_label_self_assigned
                }
                message += String.format(
                    resources.getString(assignedTextResource),
                    "<b>" + event.actor.login + "</b>"
                )
                setText(0, TypefaceUtils.ICON_PERSON)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_UNASSIGNED -> {
                var unassignedTextResource = R.string.issue_event_label_unassigned
                if (event.actor.id == event.assignee.id) {
                    unassignedTextResource = R.string.issue_event_label_self_unassigned
                }
                message += String.format(
                    resources.getString(unassignedTextResource),
                    "<b>" + event.actor.login + "</b>"
                )
                setText(0, TypefaceUtils.ICON_PERSON)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_LABELED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_label_added),
                    "<b>" + event.label.name + "</b>"
                )
                setText(0, TypefaceUtils.ICON_TAG)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_UNLABELED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_label_removed),
                    "<b>" + event.label.name + "</b>"
                )
                setText(0, TypefaceUtils.ICON_TAG)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_REFERENCED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_referenced),
                    "<b>" + event.commit_id.substring(0, 7) + "</b>"
                )
                setText(0, TypefaceUtils.ICON_BOOKMARK)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_CROSS_REFERENCED -> {
                val issue = event.source.issue
                val crossRef = issue.repository.full_name + "#" + issue.number
                message += String.format(
                    resources.getString(R.string.issue_event_cross_referenced),
                    "<b>$crossRef</b>"
                )
                setText(0, TypefaceUtils.ICON_BOOKMARK)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_REVIEW_REQUESTED -> {
                //            message += String.format(resources.getString(R.string.issue_event_review_requested), "<b>" + event.requested_reviewer.login + "</b>");
                setText(0, TypefaceUtils.ICON_EYE)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_REVIEW_REQUEST_REMOVED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_review_request_removed),
                    "<b>" + event.requested_reviewer.login + "</b>"
                )
                setText(0, TypefaceUtils.ICON_X)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_REVIEWED -> when (event.state) {
                TimelineEvent.STATE_PENDING -> {
                    message += resources.getString(R.string.issue_event_review_pending)
                    setText(0, TypefaceUtils.ICON_EYE)
                    textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
                }
                TimelineEvent.STATE_COMMENTED -> {
                    message += resources.getString(R.string.issue_event_reviewed)
                    setText(0, TypefaceUtils.ICON_EYE)
                    textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
                }
                TimelineEvent.STATE_CHANGES_REQUESTED -> {
                    message += resources.getString(R.string.issue_event_reviewed)
                    setText(0, TypefaceUtils.ICON_X)
                    textView(0).setTextColor(resources.getColor(R.color.issue_event_red))
                }
                TimelineEvent.STATE_APPROVED -> {
                    message += resources.getString(R.string.issue_event_reviewed)
                    setText(0, TypefaceUtils.ICON_CHECK)
                    textView(0).setTextColor(resources.getColor(R.color.issue_event_green))
                }
                TimelineEvent.STATE_DISMISSED -> {
                    message += resources.getString(R.string.issue_event_reviewed)
                    setText(0, TypefaceUtils.ICON_EYE)
                    textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
                }
                else -> {
                    message += resources.getString(R.string.issue_event_reviewed)
                    setText(0, TypefaceUtils.ICON_EYE)
                    textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
                }
            }
            TimelineEvent.EVENT_REVIEW_DISMISSED -> {
                message += resources.getString(R.string.issue_event_review_dismissed)
                setText(0, TypefaceUtils.ICON_X)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_MILESTONED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_milestone_added),
                    "<b>" + event.milestone.title + "</b>"
                )
                setText(0, TypefaceUtils.ICON_MILESTONE)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_DEMILESTONED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_milestone_removed),
                    "<b>" + event.milestone.title + "</b>"
                )
                setText(0, TypefaceUtils.ICON_MILESTONE)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_CLOSED -> {
                message += if (event.commit_id == null) {
                    resources.getString(R.string.issue_event_closed)
                } else {
                    String.format(
                        resources.getString(R.string.issue_event_closed_from_commit),
                        "<b>" + event.commit_id.substring(0, 7) + "</b>"
                    )
                }
                setText(0, TypefaceUtils.ICON_CIRCLE_SLASH)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_red))
            }
            TimelineEvent.EVENT_REOPENED -> {
                message += resources.getString(R.string.issue_event_reopened)
                setText(0, TypefaceUtils.ICON_PRIMITIVE_DOT)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_green))
            }
            TimelineEvent.EVENT_RENAMED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_rename),
                    "<b>" + event.rename.from + "</b>",
                    "<b>" + event.rename.to + "</b>"
                )
                setText(0, TypefaceUtils.ICON_PENCIL)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_MERGED -> {
                message += String.format(
                    resources.getString(R.string.issue_event_merged),
                    "<b>" + event.commit_id.substring(0, 7) + "</b>"
                )
                setText(0, TypefaceUtils.ICON_GIT_MERGE)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_purple))
            }
            TimelineEvent.EVENT_COMMITTED -> {
                setGone(2, true)
                message = String.format("<b>%s</b> ", event.author.name) + event.message
                setText(0, TypefaceUtils.ICON_GIT_COMMIT)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_normal))
            }
            TimelineEvent.EVENT_COMMIT_COMMENTED, TimelineEvent.EVENT_LINE_COMMENTED -> {
                message += resources.getString(R.string.issue_event_comment_diff)
                setText(0, TypefaceUtils.ICON_CODE)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_light))
            }
            TimelineEvent.EVENT_LOCKED -> {
                message += resources.getString(R.string.issue_event_lock)
                setText(0, TypefaceUtils.ICON_LOCK)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_dark))
            }
            TimelineEvent.EVENT_UNLOCKED -> {
                message += resources.getString(R.string.issue_event_unlock)
                setText(0, TypefaceUtils.ICON_KEY)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_dark))
            }
            TimelineEvent.EVENT_HEAD_REF_DELETED -> {
                message += resources.getString(R.string.issue_event_head_ref_deleted)
                setText(0, TypefaceUtils.ICON_GIT_BRANCH)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_light))
            }
            TimelineEvent.EVENT_HEAD_REF_RESTORED -> {
                message += resources.getString(R.string.issue_event_head_ref_restored)
                setText(0, TypefaceUtils.ICON_GIT_BRANCH)
                textView(0).setTextColor(resources.getColor(R.color.issue_event_light))
            }
        }
        val date: Date?
        date = when (eventString) {
            TimelineEvent.EVENT_REVIEWED -> event.submitted_at
            else -> event.created_at
        }
        if (date != null) {
            message += " " + TimeUtils.getRelativeTime(date)
        }
        setText(1, Html.fromHtml(message))
    }

    private fun updateComment(comment: TimelineEvent) {
        imageGetter.bind(textView(0), comment.body_html, comment.id)
        avatars.bind(imageView(4), comment.actor)
        imageView(4).setOnClickListener {
            context.startActivity(
                UserViewActivity.createIntent(
                    comment.actor
                )
            )
        }
        setText(1, if (comment.actor == null) "ghost" else comment.actor.login)
        setText(2, TimeUtils.getRelativeTime(comment.created_at))
        setGone(3, !comment.updated_at.after(comment.created_at))
        val canEdit = isCollaborator ||
            comment.actor != null && comment.actor.login == user
        if (canEdit) { // Edit button
            setGone(5, false)
            view<View>(5).setOnClickListener { issueFragment.editComment(comment.oldModel) }
            // Delete button
            setGone(6, false)
            view<View>(6).setOnClickListener { issueFragment.deleteComment(comment.oldModel) }
        } else {
            setGone(5, true)
            setGone(6, true)
        }
        (view<View>(7) as ReactionsView).setReactionSummary(comment.reactions)
    }

    fun setItems(items: Collection<TimelineEvent>?): MultiTypeAdapter {
        if (items == null || items.isEmpty()) return this
        this.clear()
        for (item in items) {
            if (TimelineEvent.EVENT_COMMENTED == item.event) {
                addItem(VIEW_COMMENT, item)
            } else {
                addItem(VIEW_EVENT, item)
            }
        }
        notifyDataSetChanged()
        return this
    }

    override fun initialize(type: Int, view: View): View {
        var view = view
        view = super.initialize(type, view)
        when (type) {
            VIEW_COMMENT -> {
                textView(view, 0).movementMethod = LinkMovementMethod.getInstance()
                TypefaceUtils.setOcticons(
                    textView(view, 5),
                    textView(view, 6)
                )
                setText(view, 5, TypefaceUtils.ICON_PENCIL)
                setText(view, 6, TypefaceUtils.ICON_X)
            }
            VIEW_EVENT -> TypefaceUtils.setOcticons(
                textView(view, 0)
            )
        }
        return view
    }

    override fun getViewTypeCount(): Int {
        return VIEW_TOTAL
    }

    override fun getChildLayoutId(type: Int): Int {
        return if (type == VIEW_COMMENT) R.layout.comment_item else R.layout.comment_event_item
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun isEnabled(position: Int): Boolean {
        val event = getItem(position) as TimelineEvent
        return if (TimelineEvent.EVENT_CLOSED == event.event) {
            event.commit_id != null
        } else CLICKABLE_EVENTS.contains(event.event)
    }

    override fun getChildViewIds(type: Int): IntArray {
        return if (type == VIEW_COMMENT) intArrayOf(
            R.id.tv_comment_body, R.id.tv_comment_author,
            R.id.tv_comment_date, R.id.tv_comment_edited, R.id.iv_avatar,
            R.id.iv_comment_edit, R.id.iv_comment_delete, R.id.rv_comment_reaction
        ) else intArrayOf(R.id.tv_event_icon, R.id.tv_event, R.id.iv_avatar)
    }

    companion object {
        private const val VIEW_COMMENT = 0
        private const val VIEW_EVENT = 1
        private const val VIEW_TOTAL = 2
        private val CLICKABLE_EVENTS =
            Arrays.asList(
                TimelineEvent.EVENT_CLOSED,
                TimelineEvent.EVENT_CROSS_REFERENCED,
                TimelineEvent.EVENT_MERGED,
                TimelineEvent.EVENT_REFERENCED
            )
    }

    /**
     * Create list adapter
     *
     * @param activity
     * @param avatars
     * @param imageGetter
     * @param issueFragment
     */
    init {
        context = activity
        resources = activity.resources
        this.avatars = avatars
        this.imageGetter = imageGetter
        this.issueFragment = issueFragment
        this.isCollaborator = isCollaborator
        user = loggedUser
    }
}