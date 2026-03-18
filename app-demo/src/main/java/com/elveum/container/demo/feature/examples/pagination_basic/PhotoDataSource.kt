package com.elveum.container.demo.feature.examples.pagination_basic

import com.elveum.container.demo.feature.examples.pagination_basic.PhotoRepository.PagedResult
import com.elveum.container.demo.feature.examples.pagination_basic.PhotoRepository.Photo
import com.github.javafaker.Faker
import kotlinx.coroutines.delay
import javax.inject.Inject

class PhotoDataSource @Inject constructor(
    private val faker: Faker,
) {

    private val allPhotos = List(100) {
        Photo(
            id = it + 1,
            title = faker.elderScrolls().city(),
            description = faker.lorem().sentence(4, 4),
            url = URLS[it % URLS.size],
        )
    }

    suspend fun fetchPage(pageKey: Int?): PagedResult {
        delay(2000)
        val page = pageKey ?: 0
        val start = page * PAGE_SIZE
        val end = minOf(start + PAGE_SIZE, allPhotos.size)
        val photos = allPhotos.subList(start, end)
        val nextKey = if (end < allPhotos.size) page + 1 else null
        return PagedResult(photos, nextKey)
    }

}

private const val PAGE_SIZE = 10

private val URLS = listOf(
    "https://fastly.picsum.photos/id/208/300/220.jpg?hmac=F8zK2FGmaJhH2ggCtYvXKLk-o3lQVlca2tjxgbpy2EQ",
    "https://fastly.picsum.photos/id/998/300/260.jpg?hmac=I0vwO-xcGIpyzKLj5U2eIbkUdnxwSndt0dEpj3iKqBo",
    "https://fastly.picsum.photos/id/95/300/180.jpg?hmac=13YQ-DnGS_AZKKB1BFNGs7Yb4u51Et_2tLEZh4PScLA",
    "https://fastly.picsum.photos/id/140/300/240.jpg?hmac=DEinmvvkGmyP5LaJRjlBNfBvaWCm0YdJN3U2HCjUV2Y",
    "https://fastly.picsum.photos/id/446/300/200.jpg?hmac=NmjnliRkFH1mEY2HV6dMfGJGmYwZvwMVBeI3-m7455w",
    "https://fastly.picsum.photos/id/932/300/280.jpg?hmac=TkkmApdD3MbQfP0Nv-r6n6PmwJorYUb7as5WBRSMWhQ",
    "https://fastly.picsum.photos/id/608/300/180.jpg?hmac=pj_mbKB5RA-G9Bh26PIvdNDd_6FP1kd9o_nb_rIRCiA",
    "https://fastly.picsum.photos/id/1047/300/220.jpg?hmac=Ugf55xnABHDOls0gSXNbHf_mkpK9TuAK6GgYm2oSDzE",
    "https://fastly.picsum.photos/id/900/300/200.jpg?hmac=yvAWhi_v9RR8TASc_LVRclRB0USNLeL_3f1rKhzv2h4",
    "https://fastly.picsum.photos/id/193/300/260.jpg?hmac=oIfX8n4FCkYMzhOGoNAf4gbVt5zSQ93SimGkwPo7m10",
    "https://fastly.picsum.photos/id/1018/300/240.jpg?hmac=g36jCsgA_KT1hkSmian8HRM4yVX4uBtjK7iqoNsoWpU",
    "https://fastly.picsum.photos/id/69/300/180.jpg?hmac=V2cLNWrsJ99HgkhX2GtlXTiSmlf6wea7JA5kM2sQfBQ",
    "https://fastly.picsum.photos/id/69/300/180.jpg?hmac=V2cLNWrsJ99HgkhX2GtlXTiSmlf6wea7JA5kM2sQfBQ",
    "https://fastly.picsum.photos/id/847/300/220.jpg?hmac=mWGL9xl0CABytMFN3Fd3cHVMrV_yIBI9jas0S7viQps",
    "https://fastly.picsum.photos/id/103/300/200.jpg?hmac=dUtthaBv_rM-z3sY5gWcELrBMKq05_UxucDQN3ng444",
    "https://fastly.picsum.photos/id/626/300/280.jpg?hmac=dbfypLERKqTZAhEvj4X900pGxR-mFmV4FaOPxs1XDVU",
)
