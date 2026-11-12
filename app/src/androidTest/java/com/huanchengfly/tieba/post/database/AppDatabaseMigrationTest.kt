package com.huanchengfly.tieba.post.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room 迁移回归测试：从 v39 的真实 schema 出发，逐级迁移到 v42，
 * 校验表结构、种子数据保留与 IM 新表可用性。
 *
 * 迁移路径必须与 [DatabaseModule.ALL_MIGRATIONS] 完全一致；
 * 新增版本时补一条迁移 + 一个用例，避免「只在模拟器装过新包」的假通过。
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private fun createV39Database(name: String) {
        // createDatabase 以 manifest 声明的最低版本建库，再手动提到 39
        val db = helper.createDatabase(name, 39)
        // 种子：老账号（含可空列 NULL）+ 历史 + 屏蔽 + 搜索历史
        db.execSQL(
            """
            INSERT INTO account (uid, name, bduss, tbs, portrait, sToken, cookie, nameShow, loadSuccess)
            VALUES ('10086', '测试吧友', 'BDUSS_TEST', 'TBS_TEST', 'portrait', 'STOKEN_TEST', 'cookie', '测试昵称', 1)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO history (title, data, type, timestamp, count)
            VALUES ('标题', 'data', 0, 1731400000, 3)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO block (category, type, keywords, username, uid, isRegex)
            VALUES (0, 0, '关键词', '某人', '10087', 0)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO searchhistory (content, timestamp)
            VALUES ('贴吧', 1731400001)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO draft (hash, content) VALUES ('hash1', '草稿内容')
            """.trimIndent()
        )
        db.close()
    }

    /** v39 → v42 全链路：结构通过 Room 校验，种子数据完整保留 */
    @Test
    fun migrate39To42_keepsDataAndCreatesImTables() = runBlocking {
        val dbName = "migr-39-42.db"
        createV39Database(dbName)

        val db = helper.runMigrationsAndValidate(dbName, 42, true, *DatabaseModule.ALL_MIGRATIONS)

        // account：39 的 sToken 列在 38/39 重建后仍在，数据保留
        db.query("SELECT uid, name, bduss, stoken, nameshow, loadSuccess FROM account").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("10086", c.getString(0))
            assertEquals("测试吧友", c.getString(1))
            assertEquals("BDUSS_TEST", c.getString(2))
            assertEquals("STOKEN_TEST", c.getString(3))
            assertEquals("测试昵称", c.getString(4))
            assertEquals(1L, c.getLong(5))
            assertTrue(!c.moveToNext())
        }

        // draft：39_40 去重重建，原数据保留
        db.query("SELECT hash, content FROM draft").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("hash1", c.getString(0))
            assertEquals("草稿内容", c.getString(1))
        }

        // history / block / searchhistory：重建后数据保留
        db.query("SELECT count(*) FROM history").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(1L, c.getLong(0))
        }
        db.query("SELECT keywords, isRegex FROM block").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("关键词", c.getString(0))
            assertEquals(0L, c.getLong(1))
        }
        db.query("SELECT content FROM searchhistory").use { c ->
            assertTrue(c.moveToFirst()); assertEquals("贴吧", c.getString(0))
        }

        // v41 新增的表情表与 v42 的 IM 表可写（含唯一索引）
        db.execSQL(
            "INSERT INTO meme_emoticon (uid, picId, pkgId, picUrl, width, height, type, orderId, addedTime) " +
                "VALUES ('10086', 'p1', 'pkg1', 'http://x', 100, 100, 0, 0, 1)"
        )
        db.execSQL(
            "INSERT INTO im_session (uid, peerUid, peerName, peerPortrait, lastMsgId, lastMsgTime, lastMsgPreview, lastMsgFromMe, unreadCount, peerReadMsgId) " +
                "VALUES ('10086', 123456, '对方', 'portrait', 9, 1731400002, '你好', 0, 1, 0)"
        )
        db.execSQL(
            "INSERT INTO im_message (uid, peerUid, groupId, msgId, msgType, content, createTime, fromUid, isMine) " +
                "VALUES ('10086', 123456, 777, 9, 1, '你好', 1731400002, 123456, 0)"
        )
        db.query("SELECT unreadCount FROM im_session WHERE uid='10086' AND peerUid=123456").use { c ->
            assertTrue(c.moveToFirst()); assertEquals(1L, c.getLong(0))
        }
        db.close()
    }

    /** v41 → v42：老用户升级路径，只新增 IM 表 */
    @Test
    fun migrate41To42_createsImTables() = runBlocking {
        val dbName = "migr-41-42.db"
        // 先走 39→41 得到一个真实 v41 库
        createV39Database(dbName)
        helper.runMigrationsAndValidate(dbName, 41, true, *DatabaseModule.ALL_MIGRATIONS).close()

        val db = helper.runMigrationsAndValidate(dbName, 42, true, DatabaseModule.ALL_MIGRATIONS[3])
        db.execSQL(
            "INSERT INTO im_message (uid, peerUid, groupId, msgId, msgType, content, createTime, fromUid, isMine) " +
                "VALUES ('u1', 1, 1, 1, 1, 'a', 1, 1, 0)"
        )
        // 唯一索引生效：同 (uid, peerUid, msgId) 再插应失败
        val duplicate = runCatching {
            db.execSQL(
                "INSERT INTO im_message (uid, peerUid, groupId, msgId, msgType, content, createTime, fromUid, isMine) " +
                    "VALUES ('u1', 1, 1, 1, 1, 'b', 2, 1, 0)"
            )
        }
        assertTrue("重复 msgId 应触发唯一索引冲突", duplicate.isFailure)
        db.close()
    }
}
