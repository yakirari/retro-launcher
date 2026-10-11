package com.ariy.retrolauncher.launch

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkSuggestionServiceTest {
  @Test fun bingMetadataBindsEachImageToItsOwnTitleAndRejectsUntitledUrls() {
    val html = """
      <a m='{&quot;murl&quot;:&quot;https://example.org/wrong.png&quot;,&quot;t&quot;:&quot;Unrelated game artwork&quot;}'></a>
      <a m='{&quot;murl&quot;:&quot;https://example.org/right.png&quot;,&quot;t&quot;:&quot;Animal Well official game artwork&quot;}'></a>
      <a m='{&quot;murl&quot;:&quot;https://example.org/right.jpg?size=large&amp;v=1&quot;,&quot;t&quot;:&quot;Animal Well cover art&quot;}'></a>
      <a m='{&quot;murl&quot;:&quot;https://example.org/no-title.png&quot;}'></a>
      <script>"murl":"https://example.org/random-banner.png"</script>
    """.trimIndent()
    val parsed = ArtworkSuggestionService.titledBingCandidates(html)
    assertEquals(3, parsed.size)
    val relevant = parsed.filter { ArtworkSuggestionService.isRelevantArtworkTitle("Animal Well game artwork", it.title) }
    assertEquals(listOf("https://example.org/right.png", "https://example.org/right.jpg?size=large&v=1"), relevant.map { it.imageUrl })
    assertEquals("Animal Well official game artwork", relevant.first().title)
  }

  @Test fun googleImagesMustHaveRelevantAltMetadataNotJustAnImageUrl() {
    val html = """
      <img src="https://example.org/random.png" />
      <img alt="Unrelated game" src="https://example.org/unrelated.jpg" />
      <img alt="Animal Well cover art" src="https://example.org/animal.jpg" />
      <img alt="Animal Well cover art" src="data:image/jpeg;base64,noise" />
      <img alt="Image" src="https://example.org/generic.png" />
    """.trimIndent()
    val relevant = ArtworkSuggestionService.titledImageCandidates(html, "Google Images")
      .filter { ArtworkSuggestionService.isRelevantArtworkTitle("Animal Well cover art", it.title) }
    assertEquals(listOf("https://example.org/animal.jpg"), relevant.map { it.imageUrl })
    assertTrue(ArtworkSuggestionService.titledBingCandidates("<img src='https://example.org/random.png'>").isEmpty())
  }

  @Test fun onlyWebResultsUseRankedSelectionInsteadOfShapeSelection() {
    assertTrue(ArtworkSuggestionService.isWebCandidate(ArtworkCandidate("Animal Well", "Bing Images", "https://example.org/art.png")))
    assertTrue(!ArtworkSuggestionService.isWebCandidate(ArtworkCandidate("Animal Well", "Steam Store · exact title · portrait", "https://example.org/art.png")))
    assertTrue(!ArtworkSuggestionService.isWebCandidate(ArtworkCandidate("Animal Well", "Libretro box-art catalog", "https://example.org/art.png")))
  }
  @Test
  fun negativeSearchTermsAreNotRequiredInResultTitles() {
    for (title in listOf("Ato", "Animal Well", "Cyber Protocol", "198X")) {
      val query = "$title Nintendo Switch game artwork -box -case -package -sealed -listing"
      assertTrue(ArtworkSuggestionService.isRelevantArtworkTitle(query, "$title official game artwork"))
      assertTrue(!ArtworkSuggestionService.isRelevantArtworkTitle(query, "Unrelated Nintendo Switch game artwork"))
    }
  }

    @Test
    fun physicalProductListingsAreRejectedButCoverResultsRemainEligible() {
        assertTrue(ArtworkSuggestionService.isPhysicalProductSearchResult("Brand new sealed game - for sale"))
        assertTrue(ArtworkSuggestionService.isPhysicalProductSearchResult("Game case only"))
        assertTrue(!ArtworkSuggestionService.isPhysicalProductSearchResult("Nintendo Switch front cover scan"))
    }

    @Test
    fun filtersUnrelatedFallbackResultsFor198x() {
        assertTrue(ArtworkSuggestionService.isRelevantArtworkTitle("198X Nintendo Switch game artwork", "198X - Nintendo Switch"))
        assertTrue(!ArtworkSuggestionService.isRelevantArtworkTitle("198X Nintendo Switch game artwork", "Nintendo Switch 2 cover art"))
    }

    @Test
    fun atoResultsNeedGameOrPlatformContext() {
      assertTrue(ArtworkSuggestionService.isRelevantArtworkTitle("Ato Nintendo Switch game artwork", "Ato official artwork"))
      assertTrue(!ArtworkSuggestionService.isRelevantArtworkTitle("Ato Nintendo Switch game artwork", "ATO Store Pokemon promotion"))
    }

    @Test
    fun sequelNumberMustMatchInFallbackResultTitle() {
      assertTrue(ArtworkSuggestionService.isRelevantArtworkTitle("Blasphemous 2 Nintendo Switch game artwork", "Blasphemous 2 official game artwork"))
      assertTrue(!ArtworkSuggestionService.isRelevantArtworkTitle("Blasphemous 2 Nintendo Switch game artwork", "Blasphemous official game artwork"))
    }

    @Test
    fun extractImageUrlsFromHtml_keeps_real_image_hits_from_bing_results() {
        val html = """
            <html>
              <script>
                "murl":"https://images.example.com/boxart.png?size=large&v=1",
                "imgurl":"https://images.example.com/boxart.webp",
                "turl":"https://ts1.mm.bing.net/th?id=OIP.foo&pid=15.1"
              </script>
              <img src="https://example.com/brand/logo.png" />
            </html>
        """.trimIndent()

        val urls = ArtworkSuggestionService.extractImageUrlsFromHtml(html)

        assertTrue(urls.isNotEmpty())
        assertTrue(urls.any { it.contains("images.example.com/boxart") })
    }

    @Test
    fun extractImageUrlsFromHtml_prefers_result_images_over_branding_logos() {
        val html = """
            <html>
              <img src="https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png" />
              <script>
                "murl":"https://images.example.com/boxart.png?size=large&v=1",
                "imgurl":"https://images.example.com/boxart.webp",
                "turl":"https://ts1.mm.bing.net/th?id=OIP.foo&pid=15.1"
              </script>
            </html>
        """.trimIndent()

        val urls = ArtworkSuggestionService.extractImageUrlsFromHtml(html)

        assertTrue(urls.isNotEmpty())
        assertTrue(urls.first().contains("images.example.com/boxart"))
    }
}
