import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class YoutubeToMp3 {

	private static String secretKey = "AIzaSyASWp_x6NjmAns4y1c4zA9_YbxePHpnZdc";
	/**
	 * Define a global variable that identifies the name of a file that contains the
	 * developer's API key.
	 */
	private static String appName = "Secretify";
	private static String PROPERTIES_FILENAME = "youtube.properties";

	// we only want the first result, most likely the wanted search result
	private final static long NUMBER_OF_VIDEOS_RETURNED = 1;

	/**
	 * Define a global instance of a Youtube object, which will be used to make
	 * YouTube Data API requests.
	 */
	private static YouTube youtube;

	/**
	 * Initialize a YouTube object to search for videos on YouTube. Then display the
	 * name and thumbnail image of each video in the result set.
	 *
	 * @param args command line args.
	 * @throws Exception
	 */

	public static void main(String[] args) {

		// load the youtube api
		loadYoutubeAPI();
		// by design, the program will close after one donwload
		System.out.println("Enter keywords to find a Youtube video or enter a Youtube video link.");

		// scan the input
		Scanner scanner = new Scanner(System.in);
		String input = scanner.nextLine();
		//download the video linked with the input
		//no input will download youtubes top featured video
		searchAndDownload(input);
		scanner.close();

	}

	private static void loadYoutubeAPI() {
		// Read the developer key from the properties file.
		Properties properties = new Properties();
		try {
			InputStream in = YoutubeToMp3.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			System.out.println("Could not find youtube.properties");
			System.exit(1);
		}

		try {
			// This object is used to make YouTube Data API requests. The last
			// argument is required, but since we don't need anything
			// initialized when the HttpRequest is initialized, we override
			// the interface and provide a no-op function.
			youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
				public void initialize(HttpRequest request) throws IOException {
				}

			}).setApplicationName(appName).build();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}

	private static void searchAndDownload(String keyword) {
		try {
			// Define the API request for retrieving search results.
			YouTube.Search.List search = youtube.search().list("id,snippet");
			search.setKey(secretKey);
			search.setQ(keyword);
			search.setType("video");

			// To increase efficiency, only retrieve the fields that the
			// application uses.
			search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
			search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

			// Call the API and get the YT link
			SearchListResponse searchResponse = search.execute();
			List<SearchResult> searchResultList = searchResponse.getItems();
			String URL = null;
			if (searchResultList != null) {
				URL = "https://www.youtube.com/watch?v=" + searchResultList.listIterator().next().getId().getVideoId();
			}
			download(URL, searchResultList.listIterator().next().getId().getVideoId(),
					searchResultList.listIterator().next().getSnippet().getTitle());
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}

	private static void download(String URL, String videoId, String name) throws IOException, InterruptedException {
		// download the song to the root dir
		ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "youtube-dl -x --audio-format mp3 " + URL);
		Process process = processBuilder.start();

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}

		int exitCode = process.waitFor();
		System.out.println("\nExited with error code : " + exitCode);
		if (exitCode != 0) {
			System.exit(1);
		}
		// the mp3 is now downloaded, lets rename it and return its path
		File f1 = new File("./");
		File[] matchingFiles = f1.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.contains(videoId);
			}
		});

		if (matchingFiles.length != 0) {
			matchingFiles[0].renameTo(new File(name + ".mp3"));
		}

	}
}
//TODO: get new secret key
//TODO: push
