package com.huanchengfly.tieba.post.models

data class EmoticonCache(
    /** 映射表版本：低于当前版本的旧缓存不再信任 name/id 对照（见 EmoticonManager.MAPPING_VERSION） */
    var version: Int = 0,
    var ids: List<String> = emptyList(),
    var mapping: Map<String, String> = emptyMap()
)
