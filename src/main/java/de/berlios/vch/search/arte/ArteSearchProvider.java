package de.berlios.vch.search.arte;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.jsoup.nodes.Element;
import org.osgi.framework.ServiceException;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class ArteSearchProvider implements ISearchProvider {

    public static final String CHARSET = "UTF-8";
    public static final String BASE_URI = "http://videos.arte.tv";

    @Requires(filter = "(instance.name=vch.parser.arte)")
    private IWebParser parser;

    @Override
    public String getName() {
        return parser.getTitle();
    }

    @Override
    public IOverviewPage search(String query) throws Exception {
        if (parser == null) {
            throw new ServiceException("Arte+7 Parser is not available");
        }

        // execute the search
        IOverviewPage root = parser.getRoot();
        String[] words = query.trim().toLowerCase().split("\\s");
        IOverviewPage result = new OverviewPage();
        result.setParser(parser.getId());
        result.setUri(new URI("search://arte/" + URLEncoder.encode(query, CHARSET)));
        search(result, root, words);
        return result;
    }

    @Override
    public String getId() {
        return parser.getId();
    }

    private void search(IOverviewPage result, IWebPage page, String[] query) throws Exception {
        if (parser == null) {
            throw new ServiceException("Arte+7 Parser is not available");
        }

        if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            for (String word : query) {
                if (video.getTitle().toLowerCase().contains(word) || video.getDescription().toLowerCase().contains(word)) {
                    result.getPages().add(video);
                    continue;
                }
            }
        } else if (page instanceof IOverviewPage) {
            IOverviewPage overview = (IOverviewPage) page;
            for (IWebPage child : overview.getPages()) {
                search(result, child, query);
            }
        }
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
            Element videoContainer = HtmlParserUtils.getTag(content, "div[arte_vp_url]");
            String videoUri = videoContainer.attr("arte_vp_url");
            ((IVideoPage) page).setVideoUri(new URI(videoUri));
            page = parser.parse(page);
        }
        return page;
    }
}
