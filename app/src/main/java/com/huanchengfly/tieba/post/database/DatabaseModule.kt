package com.huanchengfly.tieba.post.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_40_41 = Migration(40, 41) { db ->
    db.execSQL("CREATE TABLE IF NOT EXISTS `meme_emoticon` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `uid` TEXT NOT NULL, `picId` TEXT NOT NULL, `pkgId` TEXT NOT NULL, `picUrl` TEXT NOT NULL, `thumbnail` TEXT, `width` INTEGER NOT NULL, `height` INTEGER NOT NULL, `type` INTEGER NOT NULL, `orderId` INTEGER NOT NULL, `addedTime` INTEGER NOT NULL)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_meme_emoticon_uid_picId_pkgId_type` ON `meme_emoticon` (`uid`, `picId`, `pkgId`, `type`)")
}

private val MIGRATION_41_42 = Migration(41, 42) { db ->
    // IM 私信：会话（按对端划分）+ 消息（1v1 私信共用 priv 组，按 (uid, peerUid, msgId) 去重）
    db.execSQL("CREATE TABLE IF NOT EXISTS `im_session` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `uid` TEXT NOT NULL, `peerUid` INTEGER NOT NULL, `peerName` TEXT NOT NULL, `peerPortrait` TEXT NOT NULL, `lastMsgId` INTEGER NOT NULL, `lastMsgTime` INTEGER NOT NULL, `lastMsgPreview` TEXT NOT NULL, `lastMsgFromMe` INTEGER NOT NULL, `unreadCount` INTEGER NOT NULL, `peerReadMsgId` INTEGER NOT NULL)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_im_session_uid_peerUid` ON `im_session` (`uid`, `peerUid`)")
    db.execSQL("CREATE TABLE IF NOT EXISTS `im_message` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `uid` TEXT NOT NULL, `peerUid` INTEGER NOT NULL, `groupId` INTEGER NOT NULL, `msgId` INTEGER NOT NULL, `msgType` INTEGER NOT NULL, `content` TEXT NOT NULL, `createTime` INTEGER NOT NULL, `fromUid` INTEGER NOT NULL, `isMine` INTEGER NOT NULL)")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_im_message_uid_peerUid_msgId` ON `im_message` (`uid`, `peerUid`, `msgId`)")
}

private val MIGRATION_39_40 = Migration(39, 40) { db ->
    db.execSQL("CREATE TABLE IF NOT EXISTS `draft_new` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `hash` TEXT NOT NULL, `content` TEXT NOT NULL)")
    db.execSQL("INSERT OR REPLACE INTO `draft_new` (`id`, `hash`, `content`) SELECT `id`, `hash`, `content` FROM `draft` WHERE `id` IN (SELECT MAX(`id`) FROM `draft` GROUP BY `hash`)")
    db.execSQL("DROP TABLE IF EXISTS `draft`")
    db.execSQL("ALTER TABLE `draft_new` RENAME TO `draft`")
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_draft_hash` ON `draft` (`hash`)")
}

private val MIGRATION_38_39 = Migration(38, 39) { db ->
    // 1. 重建 account 表
    db.execSQL("ALTER TABLE `account` RENAME TO `account_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `account` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `uid` TEXT NOT NULL, `name` TEXT NOT NULL, `bduss` TEXT NOT NULL, `tbs` TEXT NOT NULL, `portrait` TEXT NOT NULL, `stoken` TEXT NOT NULL, `cookie` TEXT NOT NULL, `nameshow` TEXT, `intro` TEXT, `sex` TEXT, `fansnum` TEXT, `postnum` TEXT, `threadnum` TEXT, `concernnum` TEXT, `tbage` TEXT, `age` TEXT, `birthdayshowstatus` TEXT, `birthdaytime` TEXT, `constellation` TEXT, `tiebauid` TEXT, `loadsuccess` INTEGER NOT NULL, `uuid` TEXT, `zid` TEXT)")
    db.execSQL("INSERT INTO `account` (`id`, `uid`, `name`, `bduss`, `tbs`, `portrait`, `stoken`, `cookie`, `nameshow`, `intro`, `sex`, `fansnum`, `postnum`, `threadnum`, `concernnum`, `tbage`, `age`, `birthdayshowstatus`, `birthdaytime`, `constellation`, `tiebauid`, `loadsuccess`, `uuid`, `zid`) SELECT `id`, IFNULL(`uid`, ''), IFNULL(`name`, ''), IFNULL(`bduss`, ''), IFNULL(`tbs`, ''), IFNULL(`portrait`, ''), IFNULL(`stoken`, ''), IFNULL(`cookie`, ''), `nameshow`, `intro`, `sex`, `fansnum`, `postnum`, `threadnum`, `concernnum`, `tbage`, `age`, `birthdayshowstatus`, `birthdaytime`, `constellation`, `tiebauid`, IFNULL(`loadsuccess`, 0), `uuid`, `zid` FROM `account_old`")
    db.execSQL("DROP TABLE IF EXISTS `account_old`")

    // 2. 重建 draft 表
    db.execSQL("ALTER TABLE `draft` RENAME TO `draft_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `draft` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `hash` TEXT NOT NULL, `content` TEXT NOT NULL)")
    db.execSQL("INSERT INTO `draft` (`id`, `hash`, `content`) SELECT `id`, IFNULL(`hash`, ''), IFNULL(`content`, '') FROM `draft_old`")
    db.execSQL("DROP TABLE IF EXISTS `draft_old`")

    // 3. 重建 history 表
    db.execSQL("ALTER TABLE `history` RENAME TO `history_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `history` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `title` TEXT NOT NULL, `data` TEXT NOT NULL, `type` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `count` INTEGER NOT NULL, `extras` TEXT, `avatar` TEXT, `username` TEXT)")
    db.execSQL("INSERT INTO `history` (`id`, `title`, `data`, `type`, `timestamp`, `count`, `extras`, `avatar`, `username`) SELECT `id`, IFNULL(`title`, ''), IFNULL(`data`, ''), IFNULL(`type`, 0), IFNULL(`timestamp`, 0), IFNULL(`count`, 0), `extras`, `avatar`, `username` FROM `history_old`")
    db.execSQL("DROP TABLE IF EXISTS `history_old`")

    // 4. 重建 block 表
    db.execSQL("ALTER TABLE `block` RENAME TO `block_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `block` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `category` INTEGER NOT NULL, `type` INTEGER NOT NULL, `keywords` TEXT, `username` TEXT, `uid` TEXT, `isregex` INTEGER NOT NULL)")
    db.execSQL("INSERT INTO `block` (`id`, `category`, `type`, `keywords`, `username`, `uid`, `isregex`) SELECT `id`, IFNULL(`category`, 0), IFNULL(`type`, 0), `keywords`, `username`, `uid`, IFNULL(`isregex`, 0) FROM `block_old`")
    db.execSQL("DROP TABLE IF EXISTS `block_old`")

    // 5. 重建 topforum 表
    db.execSQL("ALTER TABLE `topforum` RENAME TO `topforum_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `topforum` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `forumid` TEXT NOT NULL)")
    db.execSQL("INSERT INTO `topforum` (`id`, `forumid`) SELECT `id`, IFNULL(`forumid`, '') FROM `topforum_old`")
    db.execSQL("DROP TABLE IF EXISTS `topforum_old`")

    // 6. 重建 searchhistory 表
    db.execSQL("ALTER TABLE `searchhistory` RENAME TO `searchhistory_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `searchhistory` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
    db.execSQL("INSERT INTO `searchhistory` (`id`, `content`, `timestamp`) SELECT `id`, IFNULL(`content`, ''), IFNULL(`timestamp`, 0) FROM `searchhistory_old`")
    db.execSQL("DROP TABLE IF EXISTS `searchhistory_old`")

    // 7. 重建 searchposthistory 表
    db.execSQL("ALTER TABLE `searchposthistory` RENAME TO `searchposthistory_old`")
    db.execSQL("CREATE TABLE IF NOT EXISTS `searchposthistory` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `content` TEXT NOT NULL, `forumname` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
    db.execSQL("INSERT INTO `searchposthistory` (`id`, `content`, `forumname`, `timestamp`) SELECT `id`, IFNULL(`content`, ''), IFNULL(`forumname`, ''), IFNULL(`timestamp`, 0) FROM `searchposthistory_old`")
    db.execSQL("DROP TABLE IF EXISTS `searchposthistory_old`")
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /** 供迁移测试使用：迁移路径与线上构建必须完全一致 */
    val ALL_MIGRATIONS = arrayOf(MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41, MIGRATION_41_42)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "tblite.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    @Provides
    fun provideAccountDao(db: AppDatabase) = db.accountDao()

    @Provides
    fun provideDraftDao(db: AppDatabase) = db.draftDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase) = db.historyDao()

    @Provides
    fun provideBlockDao(db: AppDatabase) = db.blockDao()

    @Provides
    fun provideTopForumDao(db: AppDatabase) = db.topForumDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase) = db.searchHistoryDao()

    @Provides
    fun provideSearchPostHistoryDao(db: AppDatabase) = db.searchPostHistoryDao()

    @Provides
    fun provideMemeEmoticonDao(db: AppDatabase) = db.memeEmoticonDao()
}
