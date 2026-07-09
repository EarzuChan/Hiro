package androidx.compose.foundation.lazy.layout

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable

// TIPS：这是为了某些Lazy而作的顶替

@SuppressLint("BanParcelableUsage")
private data class DefaultLazyKey(private val index: Int) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) = parcel.writeInt(index)

    override fun describeContents() = 0

    companion object {
        @Suppress("unused")
        @JvmField
        val CREATOR: Parcelable.Creator<DefaultLazyKey> = object : Parcelable.Creator<DefaultLazyKey> {
            override fun createFromParcel(parcel: Parcel) = DefaultLazyKey(parcel.readInt())

            override fun newArray(size: Int) = arrayOfNulls<DefaultLazyKey?>(size)
        }
    }
}