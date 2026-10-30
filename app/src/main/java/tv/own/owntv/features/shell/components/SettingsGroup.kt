package tv.own.owntv.features.shell.components

import tv.own.owntv.R
import tv.own.owntv.features.settings.VideoGroup

/**
 * The eight Settings groups, in the order of More › Settings' cards — a card's index is the
 * group it opens. [videos] lists the Video player groups whose rows a group shows, in page order.
 */
internal enum class SettingsGroup(val titleRes: Int, val videos: List<VideoGroup> = emptyList()) {
    QUICK(R.string.settings_group_quick),
    PROFILE(R.string.settings_group_profile),
    SOURCES(R.string.settings_group_sources),
    APPEARANCE(R.string.settings_group_appearance_layout),
    CONTENT(R.string.settings_group_content_metadata),
    PLAYER(R.string.settings_group_player_picture_sound, listOf(VideoGroup.PLAYER, VideoGroup.PICTURE, VideoGroup.SOUND)),
    LIVE(R.string.settings_group_live_recording, listOf(VideoGroup.LIVE, VideoGroup.WATCHING)),
    APP(R.string.settings_group_app),
    ;

    companion object {
        /** The group a Video player row lives in; Diagnostics sits at the end of App. */
        fun of(group: VideoGroup): SettingsGroup = entries.firstOrNull { group in it.videos } ?: APP
    }
}
