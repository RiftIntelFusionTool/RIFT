package dev.nohus.rift.network.esi

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EsiService {

    @POST("/v1/universe/ids")
    suspend fun postUniverseIds(
        @Body names: List<String>,
    ): UniverseIdsResponse

    @POST("/v3/universe/names/")
    suspend fun postUniverseNames(
        @Body ids: List<Int>,
    ): List<UniverseName>

    @GET("/v5/characters/{id}")
    suspend fun getCharactersId(
        @Path("id") characterId: Int,
    ): CharactersIdCharacter

    @GET("/v5/corporations/{id}")
    suspend fun getCorporationsId(
        @Path("id") corporationId: Int,
    ): CorporationsIdCorporation

    @GET("/v4/alliances/{id}")
    suspend fun getAlliancesId(
        @Path("id") allianceId: Int,
    ): AlliancesIdAlliance

    @GET("/v3/characters/{id}/online/")
    suspend fun getCharacterIdOnline(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharacterIdOnline

    @GET("/v2/characters/{id}/location/")
    suspend fun getCharacterIdLocation(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharacterIdLocation

    @GET("/v1/characters/{id}/wallet/")
    suspend fun getCharactersIdWallet(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): Double

    @GET("/v3/characters/{id}/search/")
    suspend fun getCharactersIdSearch(
        @Path("id") characterId: Int,
        @Query("categories") categories: String,
        @Query("strict") strict: Boolean,
        @Query("search") search: String,
        @Header("Authorization") authorization: String,
    ): CharactersIdSearch

    @GET("/v2/universe/stations/{id}/")
    suspend fun getUniverseStationsId(
        @Path("id") stationId: Int,
    ): UniverseStationsId

    @GET("/v2/universe/structures/{id}/")
    suspend fun getUniverseStructuresId(
        @Path("id") structureId: Long,
        @Header("Authorization") authorization: String,
    ): UniverseStructuresId

    @GET("/v1/universe/system_jumps/")
    suspend fun getUniverseSystemJumps(): List<UniverseSystemJumps>

    @GET("/v2/universe/system_kills/")
    suspend fun getUniverseSystemKills(): List<UniverseSystemKills>

    @GET("/v1/incursions/")
    suspend fun getIncursions(): List<Incursion>

    @GET("/v3/fw/systems/")
    suspend fun getFactionWarfareSystems(): List<FactionWarfareSystem>

    @GET("/v1/sovereignty/map/")
    suspend fun getSovereigntyMap(): List<SovereigntySystem>

    @POST("/v2/ui/autopilot/waypoint/")
    suspend fun postUiAutopilotWaypoint(
        @Query("add_to_beginning") addToBeginning: Boolean,
        @Query("clear_other_waypoints") clearOtherWaypoints: Boolean,
        @Query("destination_id") destinationId: Long,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    @GET("/v5/characters/{id}/assets/")
    suspend fun getCharactersIdAssets(
        @Path("id") characterId: Int,
        @Query("page") page: Int,
        @Header("Authorization") authorization: String,
    ): Response<List<CharactersIdAsset>>

    @POST("/v1/characters/{id}/assets/names/")
    suspend fun getCharactersIdAssetsNames(
        @Path("id") characterId: Int,
        @Body assets: List<Long>,
        @Header("Authorization") authorization: String,
    ): List<CharactersIdAssetsName>

    @GET("/v1/markets/prices/")
    suspend fun getMarketsPrices(): List<MarketsPrice>
}
