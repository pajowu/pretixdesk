package eu.pretix.pretixdesk.ui

import com.github.kittinunf.fuel.httpPost
import eu.pretix.libpretixsync.check.TicketCheckProvider
import eu.pretix.pretixdesk.PretixDeskMain
import eu.pretix.pretixdesk.VERSION
import org.json.JSONObject
import java.util.*
import kotlin.collections.List
import kotlin.collections.set


class MainController : BaseController() {

    fun toggleAsync(value: Boolean) {
        configStore.asyncModeEnabled = value
        (app as PretixDeskMain).reloadCheckProvider()
    }

    fun handleSearchInput(value: String): List<TicketCheckProvider.SearchResult>? {
        return (app as PretixDeskMain).provider.search(value)
    }

    fun handleScanInput(value: String, answers: List<TicketCheckProvider.Answer>? = null, ignore_pending: Boolean=false): TicketCheckProvider.CheckResult? {
        if (answers != null) {
            return (app as PretixDeskMain).provider.check(value, answers, ignore_pending)
        } else {
            return (app as PretixDeskMain).provider.check(value, ArrayList<TicketCheckProvider.Answer>(), ignore_pending)
        }
    }

    fun updateCheck() {
        if ((System.currentTimeMillis() - configStore.lastUpdateCheck) > 24 * 3600 * 1000) {
                        val payload = JSONObject()
            payload.put("version", VERSION)
            val request = "https://pretix.eu/.update_check/pretixdesk/".httpPost().body(payload.toString())
            request.headers["Content-Type"] = "application/json"
            val (_, response, result) = request.responseString()
            if (response.statusCode == 200) {
                try {
                    val data = JSONObject(result.get())
                    if ("ok".equals(data.optString("status"))) {
                        if (data.getJSONObject("version").getBoolean("updatable")) {
                            configStore.lastUpdateCheck = System.currentTimeMillis()
                            configStore.updateCheckNewerVersion = data.getJSONObject("version").getString("latest")
                        } else {
                            configStore.lastUpdateCheck = System.currentTimeMillis()
                            configStore.updateCheckNewerVersion = ""
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateCheckNewerVersion(): String {
        return configStore.updateCheckNewerVersion;
    }
}