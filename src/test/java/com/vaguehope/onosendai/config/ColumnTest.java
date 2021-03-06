package com.vaguehope.onosendai.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.vaguehope.onosendai.util.CollectionHelper;

public class ColumnTest {

	@Test
	public void itRoundTrips () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

	@Test
	public void itClonesWithNewId () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false);
		Column c1 = new Column(89, c);
		Column c2 = new Column(12, c1);
		assertEquals(c, c2);
	}

	@Test
	public void itClonesWithNewAccountId () throws Exception {
		final Account a1 = mock(Account.class);
		final Account a2 = mock(Account.class);
		when(a1.getId()).thenReturn("accountid");
		when(a2.getId()).thenReturn("newaccountid");

		Column c = new Column(12, "title", a1.getId(), "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false);
		Column c1 = new Column(a2, c);
		Column c2 = new Column(a1, c1);
		assertEquals(c, c2);
	}

	@Test
	public void itClonesWithNewExcludeColumnIds () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false);
		Column c1 = new Column(CollectionHelper.setOf(2), c);
		Column c2 = new Column(CollectionHelper.setOf(1, 2), c1);
		assertEquals(c, c2);
	}

	@Test
	public void itEqualsChecksExcludes () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false);
		assertFalse(c.equals(new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 1), NotificationStyle.DEFAULT, false, false)));
	}

	@Test
	public void itEqualsChecksNotify () throws Exception {
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false);
		assertFalse(c.equals(new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), null, false, false)));
	}

	@Test
	public void itParsesComplexNotificationStyle () throws Exception {
		NotificationStyle ns = new NotificationStyle(true, false, true);
		Column c = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), ns, false, false);
		String j = c.toJson().toString(2);
		Column c1 = Column.parseJson(j);
		assertEquals(c, c1);
	}

	@Test
	public void itDoesNotAllowNegativeIdsWhenParsingJson () throws Exception {
		try {
			Column.parseJson(new Column(-1, "title", "accountid", "resource", 15, null, NotificationStyle.DEFAULT, false, false).toJson().toString(2));
		}
		catch (JSONException e) {
			assertEquals("Column ID must be positive a integer.", e.getMessage());
		}
	}

	public void itDefaultsInlineMediaToFalse () throws Exception {
		JSONObject j = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, false, false).toJson();
		assertEquals(false, j.remove("inline_media"));
		Column c = Column.parseJson(j.toString(2));
		assertEquals(false, c.isInlineMedia());
	}

	public void itRoundtripsInlineMediaEnabled () throws Exception {
		JSONObject j = new Column(12, "title", "accountid", "resource", 15, CollectionHelper.setOf(1, 2), NotificationStyle.DEFAULT, true, false).toJson();
		Column c = Column.parseJson(j.toString(2));
		assertEquals(true, c.isInlineMedia());
	}

}
