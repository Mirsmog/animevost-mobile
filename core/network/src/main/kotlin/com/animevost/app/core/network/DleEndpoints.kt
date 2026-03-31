package com.animevost.app.core.network

object DleEndpoints {
    const val BASE_URL = "https://animevost.org/"
    const val MIRROR_URL = "https://v12.vost.pw/"

    // HTML pages (scraped with Jsoup)
    const val MAIN_PAGE = ""
    const val SCHEDULE = ""

    // URL patterns (for reference):
    // Detail:  /tip/{type}/{id}-{slug}.html
    // Genre:   /zhanr/{genre}/
    // Year:    /god/{year}/
    // Type:    /tip/{type}/
    // Page N:  /page/{n}/

    // Browseable listing pages
    const val ONGOING = "ongoing/"
    const val PREVIEW = "preview/"
    const val FAVORITES = "favorites/"
    const val FAVORITES_ADD = "index.php?do=favorites&doaction=add&id="
    const val FAVORITES_REMOVE = "index.php?do=favorites&doaction=del&id="

    // Search — POST with form field "story"
    const val SEARCH = "index.php?do=search"

    // DLE AJAX endpoints
    const val AJAX_RATING = "engine/ajax/rating.php"
    const val AJAX_COMMENTS = "engine/ajax/comments.php"
    const val AJAX_ADD_COMMENT = "engine/ajax/addcomments.php"
    const val AJAX_FAVORITES = "engine/ajax/favorites.php"

    // Login form submit
    const val LOGIN = "index.php?do=login"

    // Logout
    const val LOGOUT = "index.php?action=logout"

    // Video player frame
    const val PLAYER_FRAME = "frame5.php"

    // Media / CDN
    const val MEDIA_THUMBNAIL_BASE = "https://media.aniland.org/img/"

    // Default DLE skin parameter
    const val DEFAULT_SKIN = "flavor"
}
