package dev.sunls24.biliapi.http

import dev.sunls24.biliapi.http.entity.BiliResponse
import dev.sunls24.biliapi.http.entity.BiliResponseWithoutData
import dev.sunls24.biliapi.http.entity.danmaku.DanmakuData
import dev.sunls24.biliapi.http.entity.danmaku.DanmakuResponse
import dev.sunls24.biliapi.http.entity.dynamic.DynamicData
import dev.sunls24.biliapi.http.entity.history.HistoryData
import dev.sunls24.biliapi.http.entity.home.RcmdTopData
import dev.sunls24.biliapi.http.entity.region.RegionFeedRcmd
import dev.sunls24.biliapi.http.entity.search.KeywordSuggest
import dev.sunls24.biliapi.http.entity.search.SearchResultData
import dev.sunls24.biliapi.http.entity.search.WebSearchSquareData
import dev.sunls24.biliapi.http.entity.season.FollowingSeasonWebData
import dev.sunls24.biliapi.http.entity.season.SeasonFollowData
import dev.sunls24.biliapi.http.entity.season.WebSeasonData
import dev.sunls24.biliapi.http.entity.toview.ToViewData
import dev.sunls24.biliapi.http.entity.user.FollowAction
import dev.sunls24.biliapi.http.entity.user.FollowActionSource
import dev.sunls24.biliapi.http.entity.user.MyInfoData
import dev.sunls24.biliapi.http.entity.user.RelationData
import dev.sunls24.biliapi.http.entity.user.UserFollowData
import dev.sunls24.biliapi.http.entity.user.WebSpaceVideoData
import dev.sunls24.biliapi.http.entity.user.favorite.FavoriteFolderInfoListData
import dev.sunls24.biliapi.http.entity.user.favorite.UserFavoriteFoldersData
import dev.sunls24.biliapi.http.entity.video.AddCoin
import dev.sunls24.biliapi.http.entity.video.CheckSentCoin
import dev.sunls24.biliapi.http.entity.video.CheckVideoFavoured
import dev.sunls24.biliapi.http.entity.video.OneClickTripleAction
import dev.sunls24.biliapi.http.entity.video.PlayUrlData
import dev.sunls24.biliapi.http.entity.video.PlayUrlV2Data
import dev.sunls24.biliapi.http.entity.video.PopularVideoData
import dev.sunls24.biliapi.http.entity.video.SetVideoFavorite
import dev.sunls24.biliapi.http.entity.video.VideoDetail
import dev.sunls24.biliapi.http.entity.video.VideoMoreInfo
import dev.sunls24.biliapi.http.entity.web.NavResponseData
import dev.sunls24.biliapi.http.plugins.BiliUserAgent
import dev.sunls24.biliapi.http.util.encApiSign
import dev.sunls24.biliapi.http.util.injectBuvid3Cookie
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.Parameters
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.xml.parsers.DocumentBuilderFactory

@Suppress("SpellCheckingInspection")
object BiliHttpApi {
    private var endPoint: String = "api.bilibili.com"
    private lateinit var client: HttpClient

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    var wbiImgKey: String? = null
    var wbiSubKey: String? = null
    private var wbiLastRefreshDate = 0L
    private val wbiMutex = Mutex()

    // 用于获取 buvid3 的提供者，由应用层设置
    var buvid3: String = ""
        private set

    fun init(buvid3: String) {
        this.buvid3 = buvid3

        createClient()
    }

    private fun createClient() {
        client = HttpClient(OkHttp) {
            BiliUserAgent()
            install(ContentNegotiation) { json(json) }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            install(HttpRequestRetry) {
                retryOnExceptionIf(maxRetries = 2) { request, _ ->
                    request.method == HttpMethod.Get
                }
            }
            defaultRequest {
                url {
                    host = endPoint
                    protocol = URLProtocol.HTTPS
                }
            }
        }.apply {
            encApiSign()          // 1. 先注册（LIFO → 后执行）：负责签名
            injectBuvid3Cookie()  // 2. 后注册（LIFO → 先执行）：cookie 注入在签名之前
        }
    }

    /**
     * 获取热门视频列表
     */
    suspend fun getPopularVideoData(
        pageNumber: Int = 1,
        pageSize: Int = 20,
        sessData: String = ""
    ): BiliResponse<PopularVideoData> = client.get("/x/web-interface/popular") {
        parameter("pn", pageNumber)
        parameter("ps", pageSize)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取视频超详细信息
     */
    suspend fun getVideoDetail(
        av: Long? = null,
        bv: String? = null,
        sessData: String? = null
    ): BiliResponse<VideoDetail> = client.get("/x/web-interface/wbi/view/detail") {
        parameter("aid", av)
        parameter("bvid", bv)
        sessData?.let { header("Cookie", "SESSDATA=$sessData;") }
    }.body()

    /**
     * 获取视频流
     */
    suspend fun getVideoPlayUrl(
        av: Long? = null,
        bv: String? = null,
        cid: Long,
        qn: Int? = 80,
        fnval: Int? = 1,
        fnver: Int? = 0,
        fourk: Int? = 1,
        session: String? = null,
        otype: String = "json",
        type: String = "",
        platform: String = "oc",
        sessData: String? = null,
        dedeUserID: Long? = null
    ): BiliResponse<PlayUrlData> = client.get("/x/player/playurl") {
        require(av != null || bv != null) { "av and bv cannot be null at the same time" }
        parameter("avid", av)
        parameter("bvid", bv)
        parameter("cid", cid)
        parameter("qn", qn)
        parameter("fnval", fnval)
        parameter("fnver", fnver)
        parameter("fourk", fourk)
        parameter("session", session)
        parameter("otype", otype)
        parameter("type", type)
        parameter("platform", platform)
        if (sessData.isNullOrEmpty()) {
            // parameter("voice_balance", 1)
            parameter("web_location", "1315873")
            parameter("gaia_source", "pre-load")
            parameter("isGaiaAvoided", "true")
            parameter("try_look", "1")
        }
        sessData?.let { header("Cookie", "SESSDATA=$sessData;DedeUserID=$dedeUserID") }
    }.body()

    /**
     * 获取剧集视频流
     */
    suspend fun getPgcVideoPlayUrlV2(
        av: Long? = null,
        bv: String? = null,
        epid: Int? = null,
        cid: Long? = null,
        qn: Int? = null,
        fnval: Int? = null,
        fnver: Int? = null,
        fourk: Int? = null,
        session: String? = null,
        supportMultiAudio: Boolean? = null,
        drmTechType: Int? = null,
        fromClient: String? = null,
        sessData: String? = null,
        buvid3: String? = null
    ): BiliResponse<PlayUrlV2Data> = client.get("/pgc/player/web/v2/playurl") {
        av?.let { parameter("avid", it) }
        bv?.let { parameter("bvid", it) }
        epid?.let { parameter("ep_id", it) }
        cid?.let { parameter("cid", it) }
        qn?.let { parameter("qn", it) }
        fnval?.let { parameter("fnval", it) }
        fnver?.let { parameter("fnver", it) }
        fourk?.let { parameter("fourk", it) }
        session?.let { parameter("session", it) }
        supportMultiAudio?.let { parameter("support_multi_audio", it) }
        drmTechType?.let { parameter("drm_tech_type", it) }
        fromClient?.let { parameter("from_client", it) }
        val cookieParts = mutableListOf<String>()
        sessData?.let { cookieParts.add("SESSDATA=$it") }
        buvid3?.let { cookieParts.add("buvid3=$it") }
        if (cookieParts.isNotEmpty()) {
            val cookieString = cookieParts.joinToString(";")
            header("Cookie", cookieString)
        }
        //必须得加上 referer 才能通过账号身份验证
        header("referer", "https://www.bilibili.com")
    }.body()

    /**
     * 通过[cid]获取视频弹幕
     */
    suspend fun getDanmakuXml(
        cid: Long,
        sessData: String = ""
    ): DanmakuResponse {
        val xmlChannel = client.get("/x/v1/dm/list.so") {
            parameter("oid", cid)
            header("Cookie", "SESSDATA=$sessData;")
        }.bodyAsChannel()

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = withContext(Dispatchers.IO) {
            dBuilder.parse(xmlChannel.toInputStream())
        }
        doc.documentElement.normalize()

        val chatServer = doc.getElementsByTagName("chatserver").item(0).textContent
        val chatId = doc.getElementsByTagName("chatid").item(0).textContent.toLong()
        val maxLimit = doc.getElementsByTagName("maxlimit").item(0).textContent.toInt()
        val state = doc.getElementsByTagName("state").item(0).textContent.toInt()
        val realName = doc.getElementsByTagName("real_name").item(0).textContent.toInt()
        val source = runCatching {
            doc.getElementsByTagName("source").item(0).textContent
        }.getOrDefault("")

        val data = mutableListOf<DanmakuData>()
        val danmakuNodes = doc.getElementsByTagName("d")

        for (i in 0 until danmakuNodes.length) {
            val danmakuNode = danmakuNodes.item(i)
            val p = danmakuNode.attributes.item(0).textContent
            val text = danmakuNode.textContent
            data.add(DanmakuData.fromString(p, text))
        }

        return DanmakuResponse(chatServer, chatId, maxLimit, state, realName, source, data)
    }

    /**
     * 获取动态列表
     *
     * @param type 返回数据额类型 all:全部 video:视频投稿 pgc:追番追剧 article：专栏
     * @param offset 请求第2页及其之后时填写，填写上一次请求获得的offset
     */
    suspend fun getDynamicList(
        timezoneOffset: Int = -480,
        type: String = "all",
        page: Int = 1,
        offset: String? = null,
        sessData: String = ""
    ): BiliResponse<DynamicData> = client.get("/x/polymer/web-dynamic/v1/feed/all") {
        parameter("timezone_offset", timezoneOffset)
        parameter("type", type)
        parameter("page", page)
        offset?.let { parameter("offset", offset) }
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取用户[uid]的详细信息
     */
    suspend fun getUserSelfInfo(
        sessData: String = ""
    ): BiliResponse<MyInfoData> = client.get("/x/space/myinfo") {
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取截止至目标id[max]和目标时间[viewAt]历史记录
     *
     * @param business 分类 貌似无效
     * @param pageSize 页面大小
     */
    suspend fun getHistories(
        max: Long = 0,
        business: String = "",
        viewAt: Long = 0,
        pageSize: Int = 20,
        sessData: String = ""
    ): BiliResponse<HistoryData> = client.get("/x/web-interface/history/cursor") {
        parameter("max", max)
        parameter("business", business)
        parameter("view_at", viewAt)
        parameter("ps", pageSize)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取稍后再看列表
     */

    suspend fun getToView(
        sessData: String
    ): BiliResponse<ToViewData> = client.get("/x/v2/history/toview") {
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 添加视频到稍后再看
     */
    suspend fun addToView(
        avid: Long? = null,
        bvid: String? = null,
        csrf: String,
        sessData: String = ""
    ): Pair<Boolean, String> {
        val response = client.post("/x/v2/history/toview/add") {
            require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
            setBody(
                FormDataContent(
                    Parameters.build {
                        avid?.let { append("aid", "$it") }
                        bvid?.let { append("bvid", it) }
                        append("csrf", csrf)
                    }
                ))
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 移除稍后再看的视频
     */
    suspend fun delToView(
        viewed: Boolean = false,
        avid: Long? = null,
        csrf: String,
        sessData: String = ""
    ): Pair<Boolean, String> {
        val response = client.post("/x/v2/history/toview/del") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("viewed", "${if (viewed) 1 else 0}")
                        avid?.let { append("aid", "$it") }
                        append("csrf", csrf)
                    }
                ))
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 获取与视频[avid]或[bvid]有关的相关推荐视频
     */
    suspend fun getAllFavoriteFoldersInfo(
        mid: Long,
        type: Int = 0,
        rid: Long? = null,
        sessData: String
    ): BiliResponse<UserFavoriteFoldersData> = client.get("/x/v3/fav/folder/created/list-all") {
        parameter("up_mid", mid)
        parameter("type", type)
        parameter("rid", rid)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取收藏夹[mediaId]的详细内容
     *
     * @param tid 分区tid 默认为全部分区 0：全部分区
     * @param keyword 搜索关键字
     * @param order 排序方式 按收藏时间:mtime 按播放量: view 按投稿时间：pubtime
     * @param type 查询范围 0：当前收藏夹（对应media_id） 1：全部收藏夹
     * @param pageSize 每页数量 定义域：1-20
     * @param pageNumber 页码 默认为1
     * @param platform 平台标识 可为web（影响内容列表类型）
     */
    suspend fun getFavoriteList(
        mediaId: Long,
        tid: Int = 0,
        keyword: String? = null,
        order: String? = null,
        type: Int = 0,
        pageSize: Int = 20,
        pageNumber: Int = 1,
        platform: String? = null,
        sessData: String
    ): BiliResponse<FavoriteFolderInfoListData> = client.get("/x/v3/fav/resource/list") {
        parameter("media_id", mediaId)
        parameter("tid", tid)
        parameter("keyword", keyword)
        parameter("order", order)
        parameter("type", type)
        parameter("ps", pageSize)
        parameter("pn", pageNumber)
        parameter("platform", platform)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取收藏夹[mediaId]的全部内容id
     */
    suspend fun sendHeartbeat(
        avid: Long? = null,
        bvid: String? = null,
        cid: Long? = null,
        epid: Int? = null,
        sid: Int? = null,
        mid: Long? = null,
        playedTime: Int? = null,
        realtime: Int? = null,
        startTs: Long? = null,
        type: Int? = null,
        subType: Int? = null,
        dt: Int? = null,
        playType: Int? = null,
        csrf: String? = null,
        sessData: String
    ): String = client.post("/x/click-interface/web/heartbeat") {
        require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
        setBody(
            FormDataContent(
                Parameters.build {
                    avid?.let { append("aid", "$it") }
                    bvid?.let { append("bvid", it) }
                    cid?.let { append("cid", "$it") }
                    epid?.let { append("epid", "$it") }
                    sid?.let { append("sid", "$it") }
                    mid?.let { append("mid", "$it") }
                    playedTime?.let { append("played_time", "$it") }
                    realtime?.let { append("realtime", "$it") }
                    startTs?.let { append("start_ts", "$it") }
                    type?.let { append("type", "$it") }
                    subType?.let { append("sub_type", "$it") }
                    dt?.let { append("dt", "$it") }
                    playType?.let { append("play_type", "$it") }
                    csrf?.let { append("csrf", it) }
                }
            ))
        header("Cookie", "SESSDATA=$sessData;")
    }.bodyAsText()

    /**
     * 获取视频[avid]的[cid]视频更多信息，例如播放进度
     */
    suspend fun getVideoMoreInfo(
        avid: Long,
        cid: Long,
        sessData: String,
        buvid3: String
    ): BiliResponse<VideoMoreInfo> = client.get("/x/player/wbi/v2") {
        parameter("aid", avid)
        parameter("cid", cid)
        header("Cookie", "buvid3=$buvid3; SESSDATA=$sessData;")
    }.body()

    /**
     * 为视频[avid]或[bvid]点赞或取消赞
     *
     * @param like 是否点赞
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendVideoLike(
        avid: Long? = null,
        bvid: String? = null,
        like: Boolean = true,
        csrf: String,
        sessData: String
    ): Pair<Boolean, String> {
        val response = client.post("/x/web-interface/archive/like") {
            require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
            setBody(
                FormDataContent(
                    Parameters.build {
                        avid?.let { append("aid", "$it") }
                        bvid?.let { append("bvid", it) }
                        append("like", "${if (like) 1 else 2}")
                        append("csrf", csrf)
                    }
                ))
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponseWithoutData>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 检查视频[avid]或[bvid]是否已点赞
     */
    suspend fun checkVideoLiked(
        avid: Long? = null,
        bvid: String? = null,
        sessData: String
    ): Boolean {
        val response = client.get("/x/web-interface/archive/has/like") {
            require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
            avid?.let { parameter("aid", it) }
            bvid?.let { parameter("bvid", it) }
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponse<Int>>()
        return runCatching {
            response.getResponseData() == 1
        }.getOrDefault(false)
    }

    /**
     * 为视频[avid]或[bvid]点赞或取消赞
     *
     * @param like 是否顺便点赞
     * @param multiply 投币数量
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendVideoCoin(
        avid: Long? = null,
        bvid: String? = null,
        multiply: Int = 1,
        like: Boolean = false,
        csrf: String,
        sessData: String,
        buvid3: String
    ): Pair<Boolean, String> {
        require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
        val response = client.post("/x/web-interface/coin/add") {
            setBody(FormDataContent(
                Parameters.build {
                    avid?.let { append("aid", "$it") }
                    bvid?.let { append("bvid", it) }
                    append("multiply", "$multiply")
                    append("select_like", "${if (like) 1 else 0}")
                    append("csrf", csrf)
                }
            ))
            header("Cookie", "SESSDATA=$sessData;buvid3=$buvid3")
        }.body<BiliResponse<AddCoin>>()
        return Pair(response.code == 0, response.message)
    }

    /**
     * 检查视频[avid]或[bvid]是否已投币
     */
    suspend fun checkVideoSentCoin(
        avid: Long? = null,
        bvid: String? = null,
        sessData: String
    ): Boolean {
        val response = client.get("/x/web-interface/archive/coins") {
            require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
            avid?.let { parameter("aid", it) }
            bvid?.let { parameter("bvid", it) }
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponse<CheckSentCoin>>()
        return runCatching {
            response.getResponseData().multiply != 0
        }.getOrDefault(false)
    }

    /**
     * 为视频[avid]添加到[addMediaIds]或从[delMediaIds]移除
     */
    suspend fun setVideoToFavorite(
        avid: Long,
        type: Int = 2,
        addMediaIds: List<Long> = listOf(),
        delMediaIds: List<Long> = listOf(),
        csrf: String? = null,
        sessData: String
    ) {
        val response = client.post("/x/v3/fav/resource/deal") {
            require(addMediaIds.isNotEmpty() || delMediaIds.isNotEmpty()) {
                "addMediaIds and delMediaIds cannot be empty at the same time"
            }
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("rid", "$avid")
                        append("type", "$type")
                        append("add_media_ids", addMediaIds.joinToString(separator = ","))
                        append("del_media_ids", delMediaIds.joinToString(separator = ","))
                        csrf?.let { append("csrf", it) }
                    }
                ))
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponse<SetVideoFavorite>>()
        check(response.code == 0) { response.message }
    }

    /**
     * 检查视频[avid]是否已收藏
     */
    suspend fun checkVideoFavoured(
        avid: Long,
        sessData: String
    ): Boolean {
        val response = client.get("/x/v2/fav/video/favoured") {
            parameter("aid", avid)
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponse<CheckVideoFavoured>>()
        return runCatching {
            response.getResponseData().favoured
        }.getOrDefault(false)
    }

    /**
     * 为视频[avid]或[bvid]一键三连
     *
     * @param csrf bili_jct
     * @param sessData SESSDATA
     */
    suspend fun sendVideoOneClickTripleAction(
        avid: Long? = null,
        bvid: String? = null,
        csrf: String,
        sessData: String
    ): Triple<Boolean, String, OneClickTripleAction?> {
        require(avid != null || bvid != null) { "avid and bvid cannot be null at the same time" }
        val response = client.post("/x/web-interface/archive/like/triple") {
            setBody(FormDataContent(
                Parameters.build {
                    avid?.let { append("aid", "$it") }
                    bvid?.let { append("bvid", it) }
                    append("csrf", csrf)
                }
            ))
            header("Cookie", "SESSDATA=$sessData;")
        }.body<BiliResponse<OneClickTripleAction>>()
        return Triple(response.code == 0, response.message, response.data)
    }

    /**
     * 获取用户[mid]投稿视频
     *
     * @param order 排序方式 默认为pubdate 最新发布：pubdate 最多播放：click 最多收藏：stow
     * @param tid 筛选目标分区 默认为0 0：不进行分区筛选 分区tid为所筛选的分区
     * @param keyword 关键词筛选 用于使用关键词搜索该UP主视频稿件
     * @param pageNumber 页码
     * @param pageSize 每页项数 最小1，最大50
     */
    suspend fun getWebUserSpaceVideos(
        mid: Long,
        order: String = "pubdate",
        tid: Int = 0,
        keyword: String? = null,
        pageNumber: Int = 1,
        pageSize: Int = 30,
        sessData: String,
        dedeUserID: Long? = null
    ): BiliResponse<WebSpaceVideoData> = client.get("/x/space/wbi/arc/search") {
        parameter("mid", mid)
        parameter("order", order)
        parameter("tid", tid)
        keyword?.let { parameter("keyword", it) }
        parameter("pn", pageNumber)
        parameter("ps", pageSize)
        // 风控
        parameter("dm_img_list", "[]")
        parameter("dm_img_str", "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ")
        parameter("dm_cover_img_str", "QU5HTEUgKEFNRCwgQU1EIFJhZGVvbiA3ODBNIEdyYXBoaWNzICgweDAwMDAxNUJGKSBEaXJlY3QzRDExIHZzXzVfMCBwc181XzAsIEQzRDExKUdvb2dsZSBJbmMuIChBTU")
        parameter("dm_img_inter", "{\"ds\":[],\"wh\":[4769,2793,43],\"of\":[285,570,285]}")
        header("Cookie", "SESSDATA=$sessData;DedeUserID=$dedeUserID;")
        header("referer", "https://space.bilibili.com")
    }.body()

    suspend fun getWebSeasonInfo(
        seasonId: Int? = null,
        epId: Int? = null,
        sessData: String = ""
    ): BiliResponse<WebSeasonData> = client.get("/pgc/view/web/season") {
        require(seasonId != null || epId != null) { "seasonId and epId cannot be null at the same time" }
        seasonId?.let { parameter("season_id", it) }
        epId?.let { parameter("ep_id", it) }
        header("Cookie", "SESSDATA=$sessData;")
        //必须得加上 referer 才能通过账号身份验证
        header("referer", "https://www.bilibili.com")
    }.body()

    /**
     * 获取剧集[seasonId]或[epId]的详细信息 (App)，例如 ss24439 ep234533，传参仅需数字
     */
    suspend fun addSeasonFollow(
        seasonId: Int,
        csrf: String,
        sessData: String
    ): BiliResponse<SeasonFollowData> = client.post("/pgc/web/follow/add") {
        setBody(
            FormDataContent(
                Parameters.build {
                    append("season_id", "$seasonId")
                    append("csrf", csrf)
                }
            ))
        header("Cookie", "SESSDATA=$sessData;")
        //必须得加上 referer 才能通过账号身份验证
        header("referer", "https://www.bilibili.com")
    }.body()

    /**
     * 取消番剧[seasonId]的追番
     */
    suspend fun delSeasonFollow(
        seasonId: Int,
        csrf: String,
        sessData: String
    ): BiliResponse<SeasonFollowData> = client.post("/pgc/web/follow/del") {
        setBody(
            FormDataContent(
                Parameters.build {
                    append("season_id", "$seasonId")
                    append("csrf", csrf)
                }
            ))
        header("Cookie", "SESSDATA=$sessData;")
        //必须得加上 referer 才能通过账号身份验证
        header("referer", "https://www.bilibili.com")
    }.body()

    /**
     * 单独获取剧集[seasonId]的用户信息[WebSeasonData.UserStatus]
     */
    /**
     * 获取用户[mid]的关注列表，对于其他用户只能访问前5页
     */
    suspend fun getUserFollow(
        mid: Long,
        orderType: String? = null,
        pageSize: Int = 50,
        pageNumber: Int = 1,
        sessData: String
    ): BiliResponse<UserFollowData> = client.get("/x/relation/followings") {
        parameter("vmid", mid)
        orderType?.let { parameter("order_type", orderType) }
        parameter("ps", pageSize)
        parameter("pn", pageNumber)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 更改与用户[mid]之间的相互关系[action]
     */
    suspend fun modifyFollow(
        mid: Long,
        action: FollowAction,
        actionSource: FollowActionSource,
        csrf: String? = null,
        sessData: String
    ): BiliResponseWithoutData = client.post("/x/relation/modify") {
        setBody(
            FormDataContent(
                Parameters.build {
                    append("fid", "$mid")
                    append("act", "${action.id}")
                    append("re_src", "${actionSource.id}")
                    csrf?.let { append("csrf", csrf) }
                }
            ))
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取与用户[mid]的相互关系[RelationData]
     *
     * 有两个api，响应相同
     * - https://api.bilibili.com/x/space/acc/relation
     * - https://api.bilibili.com/x/web-interface/relation
     */
    suspend fun getRelations(
        mid: Long,
        sessData: String
    ): BiliResponse<RelationData> = client.get("/x/space/wbi/acc/relation") {
        parameter("mid", mid)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取用户[mid]的关系统计（关注数，粉丝数，黑名单数）
     */
    /**
     * 获取搜索提示（Web）
     *
     * @param limit 返回数量
     * @param platform 平台标识
     */
    suspend fun getWebSearchSquare(
        limit: Int = 10,
        platform: String? = null
    ): BiliResponse<WebSearchSquareData> =
        client.get("/x/web-interface/wbi/search/square") {
            parameter("limit", limit)
            platform?.let { parameter("platform", platform) }
        }.body()

    /**
     * 获取搜索提示（App）
     *
     * @param limit 返回数量，上限仅为 10
     * @param platform 平台标识
     */
    suspend fun getKeywordSuggest(
        term: String,
        mainVer: String = "v1",
        highlight: String? = null,
        buvid: String
    ): KeywordSuggest {
        // 需手动解析 json，因为返回的 Content-Type 为 null，会导致 Ktor 抛出异常
        // io.ktor.client.call.NoTransformationFoundException: Expected response body of the type 'class dev.sunls24.biliapi.http.entity.search.KeywordSuggest (Kotlin reflection is not available)' but was 'class io.ktor.utils.io.ByteBufferChannel (Kotlin reflection is not available)'
        // In response from `https://s.search.bilibili.com/main/suggest?term=xxx`
        // Response status `200 `
        // Response header `ContentType: null`
        // Request header `Accept: application/json`
        val responseText = client.get("https://s.search.bilibili.com/main/suggest") {
            parameter("term", term)
            parameter("main_ver", mainVer)
            highlight?.let { parameter("highlight", it) }
            parameter("buvid", buvid)
        }.readRawBytes().toString(Charsets.UTF_8)
        val keywordSuggest = json.decodeFromString<KeywordSuggest>(responseText)
        val result = json.decodeFromJsonElement<KeywordSuggest.Result>(keywordSuggest.result!!)
        keywordSuggest.suggests.addAll(result.tag)
        return keywordSuggest
    }

    /**
     * 综合搜索与[keyword]相关的结果
     */
    suspend fun searchType(
        keyword: String,
        type: String,
        page: Int = 1,
        tid: Int? = null,
        order: String? = null,
        duration: Int? = null,
        buvid3: String? = null
    ): BiliResponse<SearchResultData> = client.get("/x/web-interface/wbi/search/type") {
        parameter("keyword", keyword)
        parameter("search_type", type)
        parameter("page", page)
        tid?.let { parameter("tids", it) }
        order?.let { parameter("order", it) }
        duration?.let { parameter("duration", it) }
        header("Cookie", "buvid3=$buvid3;")
        header("referer", "https://search.bilibili.com/")
    }.body()

    /**
     * 获取用户[mid]的追剧列表
     *
     * @param type 追剧类型
     * @param status 追剧状态
     * @param pageNumber 页码
     * @param pageSize 每页数量 [1, 30]
     * @param mid 用户id
     */
    suspend fun getFollowingSeasons(
        type: Int,
        status: Int,
        pageNumber: Int = 1,
        pageSize: Int = 15,
        mid: Long,
        sessData: String? = ""
    ): BiliResponse<FollowingSeasonWebData> = client.get("/x/space/bangumi/follow/list") {
        parameter("type", type)
        parameter("follow_status", status)
        parameter("pn", pageNumber)
        parameter("ps", pageSize)
        parameter("vmid", mid)
        header("Cookie", "SESSDATA=$sessData;")
    }.body()

    /**
     * 获取导航栏用户信息
     *
     * 内含 wbi keys
     */
    private suspend fun getWebInterfaceNav(cookieHeader: String?): BiliResponse<NavResponseData> =
        client.get("/x/web-interface/nav") {
            cookieHeader?.let { header(HttpHeaders.Cookie, it) }
        }.body()

    suspend fun updateWbi(force: Boolean = false, cookieHeader: String? = null) {
        wbiMutex.withLock {
            val now = System.currentTimeMillis()
            val needsUpdate = force || wbiImgKey == null || wbiSubKey == null ||
                    now - wbiLastRefreshDate > 2 * 60 * 60 * 1000L
            if (!needsUpdate) return

            try {
                val wbiData = getWebInterfaceNav(cookieHeader).getResponseData().wbiImg
                wbiImgKey = wbiData.getImgKey()
                wbiSubKey = wbiData.getSubKey()
                wbiLastRefreshDate = now
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
            }
        }
    }

    private fun invalidateWbi() {
        wbiImgKey = null
        wbiSubKey = null
        wbiLastRefreshDate = 0L
    }

    /**
     * 获取首页视频推荐列表（Web）
     */
    suspend fun getFeedRcmd(
        freshType: Int = 4,
        pageSize: Int = 30,
        idx: Int = 1,
        fetchRow: Int = 1,
        lastShowlist: String? = null,
        sessData: String? = null
    ): BiliResponse<RcmdTopData> {
        suspend fun request(): BiliResponse<RcmdTopData> =
            client.get("/x/web-interface/wbi/index/top/feed/rcmd") {
                parameter("fresh_type", freshType)
                parameter("feed_version", "V8")
                parameter("homepage_ver", 1)
                parameter("ps", pageSize)
                parameter("fresh_idx", idx)
                parameter("fresh_idx_1h", idx)
                parameter("fetch_row", fetchRow)
                lastShowlist?.let { parameter("last_showlist", it) }
                sessData?.let { header("Cookie", "SESSDATA=$it;") }
            }.body()

        val response = request()
        if (response.code != -403) return response
        invalidateWbi()
        updateWbi(
            force = true,
            cookieHeader = sessData?.let { "SESSDATA=$it;" }
        )
        return request()
    }

    suspend fun downloadText(url: String): String = client.get(url).bodyAsText()

    suspend fun getRegionFeedRcmd(
        displayId: Int,
        requestCnt: Int = 15,
        fromRegion: Int,
        device: String = "web",
        plat: Int = 30,
        sessData: String? = null
    ): BiliResponse<RegionFeedRcmd> = client.get("/x/web-interface/region/feed/rcmd") {
        parameter("display_id", displayId)
        parameter("request_cnt", requestCnt)
        parameter("from_region", fromRegion)
        parameter("device", device)
        parameter("plat", plat)
        sessData?.let { header("Cookie", "SESSDATA=$it;") }
    }.body()
}
