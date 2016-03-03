package oracle.eloqua.standalone;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

public class SRMDailyReport extends DailyQualityScorecard{
	private static final String CUSTOMER_REPORTED_JQL = "filter=59340";
	private static final String SUPPORT_ESCALATED_JQL = "filter=59373";
	private static final String HOTFIX_BUGS_JQL = "filter=59370";
	private static final String BUGS_CREATED_JQL = "filter=59348";
	private static final String BUGS_RESOLVED_JQL = "filter=59351";
	private static final String ALL_BUGS_JQL = "filter=59352";

	private static String[] COMPONENTS = { overallKeyword, "Accounts",
			"Listen", "Publish", "Engage", "Analytics", "Shop", "Tabs",
			"Calendar", "Lexical", "Mobile", "Modular SRM", "Social Station" };

	private static String[] PROD_HIERARCHY = { overallKeyword, "SRMA",
			"LISTEN", "PUB", "ENGAGE", "AN", "SHOP", "TABS", "CONN", "LEXICAL",
			"SRM_MOBILE", "MODULAR", "SOCSTATION" };

	public SRMDailyReport() {
		super();
	}

	public static void main(String... args) {
		ArgumentRegistry.parse(args);

		// used for timing this method
		Date startDate = new Date();

		if (ArgumentRegistry.getRegistry().getUser() == null) {
			System.err.println("Missing -user parameter");
			return;
		}
		if (ArgumentRegistry.getRegistry().getPassword() == null) {
			System.err.println("Missing -password parameter");
			return;
		}

		SRMDailyQualityScorecard scoreCard = new SRMDailyQualityScorecard();

		scoreCard.setDebug(Boolean.valueOf(ArgumentRegistry.getRegistry().get(
				"-debug", "false")));
		scoreCard.setSkipQueries(Boolean.valueOf(ArgumentRegistry.getRegistry()
				.get("-skipQueries", "false")));
		scoreCard.setPostToConfluence(Boolean.valueOf(ArgumentRegistry
				.getRegistry().get("-postToConfluence", "true")));
		scoreCard.setCreateLocalHTML(Boolean.valueOf(ArgumentRegistry
				.getRegistry().get("-createLocalHTML", "true")));
		scoreCard.setPostToHipChat(Boolean.valueOf(ArgumentRegistry
				.getRegistry().get("-postToHipChat", "false")));
		scoreCard.setLocalFileLocation(ArgumentRegistry.getRegistry().get(
				"-localFileLocation", "D:\\SRMDailyScoreCard.html"));

		String comps = ArgumentRegistry.getRegistry().get("-components");
		if (comps != null) {
			String[] cs = comps.split(",");
			COMPONENTS = new String[cs.length];

			System.out.println("Setting components to ");
			for (int i = 0; i < cs.length; i++) {
				try {
					COMPONENTS[i] = URLDecoder.decode(cs[i], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					System.err
							.println("Error unencoding component form command line: "
									+ cs[i]);
					System.err.println("Using unencoded command line value: "
							+ cs[i]);
					COMPONENTS[i] = cs[i];
					e.printStackTrace();
				}
				System.out.print(" " + COMPONENTS[i] + " ");
			}
		}

		scoreCard.setTeams(COMPONENTS);

		String prodHier = ArgumentRegistry.getRegistry().get(
				"-productHierarchies");
		if (prodHier != null) {
			String[] cs = comps.split(",");
			PROD_HIERARCHY = new String[cs.length];

			System.out.println("Setting product hierarchies to ");
			for (int i = 0; i < cs.length; i++) {
				try {
					PROD_HIERARCHY[i] = URLDecoder.decode(cs[i], "UTF-8");
				} catch (UnsupportedEncodingException e) {
					System.err
							.println("Error unencoding product hierarchy from command line: "
									+ cs[i]);
					System.err.println("Using unencoded command line value: "
							+ cs[i]);
					PROD_HIERARCHY[i] = cs[i];
					e.printStackTrace();
				}
				System.out.print(" " + PROD_HIERARCHY[i] + " ");
			}
		}

		scoreCard.setScorecardTitles(new String[] { "Hotfix bugs",
				"Support escalated bugs", "Customer reported bugs" });

		scoreCard.setNonRDBugsFilter(ArgumentRegistry.getRegistry().get(
				"-jqlCustomerReported", CUSTOMER_REPORTED_JQL));
		scoreCard.setEscalatedBugsFilter(ArgumentRegistry.getRegistry().get(
				"-jqlSupportEscalated", SUPPORT_ESCALATED_JQL));
		scoreCard.setHotfixBugsFilter(ArgumentRegistry.getRegistry().get(
				"-jqlHotFixes", HOTFIX_BUGS_JQL));

		scoreCard.setScorecardQueries(new String[] {
				scoreCard.getHotfixBugsFilter(),
				scoreCard.getEscalatedBugsFilter(),
				scoreCard.getNonRDBugsFilter() });

		scoreCard.setBugsCreatedJQLQuery(ArgumentRegistry.getRegistry().get(
				"-jqlBugsCreated", BUGS_CREATED_JQL));
		scoreCard.setBugsResolvedJQLQuery(ArgumentRegistry.getRegistry().get(
				"-jqlBugsResolved", BUGS_RESOLVED_JQL));
		scoreCard.setAllBugsJQLQuery(ArgumentRegistry.getRegistry().get(
				"-jqlAllBugs", ALL_BUGS_JQL));

		scoreCard.setConfluenceSpace(ArgumentRegistry.getRegistry().get(
				"-confluenceSpace", "SRM"));
		scoreCard.setConfluencePageId(ArgumentRegistry.getRegistry().get(
				"-confluencePageId", "177116017"));
		scoreCard.setConfluencePageTitle(ArgumentRegistry.getRegistry().get(
				"-confluencePageTitle", "RVTest"));
		scoreCard.createScorecardAndPostToConfluence();

		// print out how long this method took
		Date endDate = new Date();
		System.out.println("\n\n\nScorecard took "
				+ (endDate.getTime() - startDate.getTime()) / (1000 * 60)
				+ " minutes");
	}

	private String getProductHierarchy(String team) {
		int count = 0;
		for (String s : COMPONENTS) {
			if (s.equals(team)) {
				return PROD_HIERARCHY[count];
			}
			count++;
		}
		return null;
	}

	@Override
	public String getProjectTeamJQL(String team) {
		if (!team.equals(getOverallKeyword())) {
			// return "component = \"" + team + "\" AND ";
			return "(component = \""
					+ team
					+ "\" OR \"Product Hierarchy\" in dbValuesMatching('Component', '"
					+ getProductHierarchy(team) + "')) " + "AND ";
		} else {
			boolean addTeams = false;
			// this is disabled because when all components are sent, then
			// JIRA errors out... can't figure out why... it seems to be
			// due to the length of the URL?
			if (addTeams) {
				String[] teams = getTeams();
				if (teams != null) {
					StringBuilder sb = new StringBuilder("component in (");
					int count = 0;
					for (String t : teams) {
						if (!t.equals(getOverallKeyword())) {
							if (count > 0) {
								sb.append(",");
							}
							sb.append("\"").append(t).append("\"");
							count++;
						}
					}
					sb.append(") AND ");
					return sb.toString();
				}
			}
		}
		return "";
	}
}
