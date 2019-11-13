package edu.illinois.cs465.spotmix.api.spotify.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/*
Copyright (c) 2019 Kotlin Data Classes Generated from JSON powered by http://www.json2kotlin.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

For support, please feel free to contact me at https://www.linkedin.com/in/syedabsar */

@Parcelize
data class Track(

    @SerializedName("album") val album: Album,
    @SerializedName("artists") val artists: List<Artist>,
    @SerializedName("available_markets") val available_markets: List<String>,
    @SerializedName("disc_number") val disc_number: Int,
    @SerializedName("duration_ms") val duration_ms: Int,
    @SerializedName("explicit") val explicit: Boolean,
    @SerializedName("external_ids") val external_ids: ExternalIds,
    @SerializedName("external_urls") val external_urls: ExternalUrls,
    @SerializedName("href") val href: String,
    @SerializedName("id") val id: String,
    @SerializedName("is_local") val is_local: Boolean,
    @SerializedName("name") val name: String,
    @SerializedName("popularity") val popularity: Int,
    @SerializedName("preview_url") val preview_url: String,
    @SerializedName("track_number") val track_number: Int,
    @SerializedName("type") val type: String,
    @SerializedName("uri") val uri: String
) : Parcelable {

    // used to reconstruct obj when reading from Firebase
    @Suppress("unused")
    constructor() : this(
        Album(),
        mutableListOf(),
        mutableListOf(),
        -1,
        -1,
        false,
        ExternalIds(),
        ExternalUrls(),
        "",
        "",
        false,
        "",
        -1,
        "",
        -1,
        "",
        ""
    )

    /**
     * Helper function to easily get displayable version of artists.
     * */
    fun getArtistNames(): String = artists.map { it.name }.reduceRight { s, acc -> "$acc, $s" }
}