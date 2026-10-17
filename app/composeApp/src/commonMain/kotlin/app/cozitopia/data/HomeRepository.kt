package app.cozitopia.data

import app.cozitopia.domain.Home
import app.cozitopia.domain.Homie
import app.cozitopia.domain.Invitation
import app.cozitopia.domain.Role
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class Membership(val home: Home, val role: Role)

data class Member(val homie: Homie, val email: String)

class HomeRepository(private val client: SupabaseClient) {
    suspend fun myHomes(userId: String): List<Membership> =
        client.from("home_members")
            .select(Columns.raw("role, homes(id, name)")) { filter { eq("user_id", userId) } }
            .decodeList<MembershipDto>()
            .map { Membership(Home(it.homes.id, it.homes.name), it.role) }
            .sortedBy { it.home.name.lowercase() }

    suspend fun createHome(name: String): String =
        client.postgrest.rpc("create_home", buildJsonObject { put("home_name", name) })
            .decodeAs<HomeDto>().id

    suspend fun rename(homeId: String, name: String) {
        client.from("homes").update({ set("name", name.trim()) }) {
            filter { eq("id", homeId) }
        }
    }

    suspend fun members(homeId: String): List<Member> =
        client.from("home_members")
            .select(Columns.raw("user_id, role, profiles(display_name, email)")) {
                filter { eq("home_id", homeId) }
            }
            .decodeList<MemberDto>()
            .map { Member(Homie(it.userId, it.profiles.displayName, it.role), it.profiles.email) }
            .sortedBy { it.homie.name.lowercase() }

    suspend fun changeRole(homeId: String, userId: String, role: Role) {
        client.from("home_members").update({ set("role", role.name) }) {
            filter {
                eq("home_id", homeId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun removeMember(homeId: String, userId: String) {
        client.from("home_members").delete {
            filter {
                eq("home_id", homeId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun pendingInvitations(email: String): List<Invitation> =
        client.from("invitations")
            .select(Columns.raw("id, home_id, email, role, homes(name)")) {
                filter {
                    eq("status", "PENDING")
                    eq("email", email.lowercase())
                }
            }
            .decodeList<InvitationDto>()
            .map { Invitation(it.id, it.homeId, it.homes.name, it.email, it.role) }

    suspend fun invite(homeId: String, email: String, role: Role) {
        client.postgrest.rpc(
            "invite_homie",
            buildJsonObject {
                put("target_home", homeId)
                put("invitee_email", email)
                put("invitee_role", role.name)
            },
        )
    }

    suspend fun accept(invitationId: String): String =
        client.postgrest.rpc("accept_invitation", buildJsonObject { put("invitation_id", invitationId) })
            .decodeAs<String>()

    suspend fun decline(invitationId: String) {
        client.postgrest.rpc("decline_invitation", buildJsonObject { put("invitation_id", invitationId) })
    }
}
