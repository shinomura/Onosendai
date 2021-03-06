package com.vaguehope.onosendai.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageHostHelper {

	private static final Pattern INSTAGRAM_URL = Pattern.compile("^http://instagram.com/p/(.+)/$");
	private static final Pattern TWITPIC_URL = Pattern.compile("^http://twitpic.com/(.+)$");
	private static final Pattern IMGUR_URL = Pattern.compile("^http://(?:i\\.)?imgur.com/(.+?)(?:\\..+)?$");
	private static final Pattern YFROG_URL = Pattern.compile("^http://yfrog.com/(.+)$");

	private ImageHostHelper () {
		throw new AssertionError();
	}

	public static String thumbUrl (final String linkUrl, final boolean hdMedia) {
		{ // http://instagram.com/developer/embedding
			final Matcher m = INSTAGRAM_URL.matcher(linkUrl);
			if (m.matches()) return linkUrl + "media/?size=" + (hdMedia ? "l" : "m");
		}

		{ // http://dev.twitpic.com/docs/thumbnails/
			final Matcher m = TWITPIC_URL.matcher(linkUrl);
			if (m.matches()) return "http://twitpic.com/show/thumb/" + m.group(1) + ".jpg";
		}

		{ // https://api.imgur.com/models/image
			final Matcher m = IMGUR_URL.matcher(linkUrl);
			if (m.matches()) {
				final String imgId = m.group(1);
				if (imgId.startsWith("a/") || imgId.startsWith("gallery/")) return null;
				return "http://i.imgur.com/" + imgId + (hdMedia ? "h" : "l") + ".jpg";
			}
		}

		{ // http://twitter.yfrog.com/page/api#a5
			final Matcher m = YFROG_URL.matcher(linkUrl);
			if (m.matches()) return "http://yfrog.com/" + m.group(1) + (hdMedia ? ":medium" : ":small");
		}

		return null;
	}

}
