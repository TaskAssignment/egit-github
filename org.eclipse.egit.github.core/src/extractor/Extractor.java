package extractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

public class Extractor {
	//------------------------------------------------------------------------------------------------------------------------
	private static final String DATASET_DIRECTORY_GH_TSV = "C:\\2-Study\\BugTriaging2\\Data Set\\GH\\AtLeastUpTo20161001\\2-TSV\\3- 13 projects + 2 project families (13 + 6 more projects)";
	private static final String TAB = "\t";
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	private static boolean downloadAndSaveBugEventsInFile(GitHubClient client, IssueService is,
			String projectId, String owner, String repo, String bugNumber, FileWriter writer, Integer numberOfEventsWritten){
//		boolean testExit = true;
//		if (testExit)
//			return true;
//		else
//			return false;

		boolean result = false;
		numberOfEventsWritten = 0;
		try{
			PageIterator<IssueEvent> pi = is.pageIssueEvents(owner, repo, Integer.parseInt(bugNumber));
			while (pi.hasNext()){
				Collection<IssueEvent> c = pi.next();//Fetching a page of bug events.
				for (IssueEvent ie:c){
					User assignee = ie.getAssignee();
					String assigneeString;
					if (assignee == null)
						assigneeString = "";
					else
						assigneeString = assignee.getLogin();

					User assigner = ie.getAssigner();
					String assignerString;
					if (assigner == null)
						assignerString = "";
					else
						assignerString = assigner.getLogin();

					Label label = ie.getLabel();
					String labelString;
					if (label == null)
						labelString = "";
					else
						labelString = label.getName();

					String commitSHAString = ie.getCommitId();
					if (commitSHAString == null)
						commitSHAString = "";

					Milestone milestone = ie.getMilestone();
					String milestoneString;
					if (milestone == null)
						milestoneString = "";
					else
						milestoneString = milestone.getTitle(); //or getDescription

					User actor = ie.getActor();
					String actorString;
					if (actor == null)
						actorString = "";
					else
						actorString = actor.getLogin(); //or getDescription

					writer.append(ie.getId() + TAB
							+ projectId + TAB
							+ bugNumber + TAB
							+ Dates.toIsoString(ie.getCreatedAt()) + TAB
							+ actorString + TAB
							+ ie.getEvent() + TAB
							+ assignerString + TAB
							+ assigneeString + TAB
							+ commitSHAString + TAB
							+ labelString + TAB
							+ milestoneString + TAB
							+ "\n");
					numberOfEventsWritten++;

//					System.out.println("eventId: " + ie.getId() + "\n"
//							+ "projectId: " + projectId + "\n"
//							+ "bugNumber: " + bugNumber + "\n"
//							+ "date: " + Dates.toIsoString(ie.getCreatedAt()) + "\n"
//							+ "actor: " + ie.getActor().getLogin() + "\n"
//							+ "typeOfEvent: " + ie.getEvent() + "\n"
//							+ "URL: " + ie.getUrl() + "\n"
//							+ "assigneer: " + assignerString + "\n"
//							+ "assignee: " + assigneeString + "\n"
//							+ "commitSHA: " + commitSHAString + "\n"
//							+ "label: " + labelString
//							+ "milestone: " + milestoneString + "\n"
//							);
//					System.out.println("-------------------------");
				}
			} //while (pi.hasNext()).

			writer.flush();
			System.out.println("#of events written for <projectId->bugNumer>: <"+projectId + "->" + bugNumber + ">: " + numberOfEventsWritten);
//			System.out.println(client.getRequestLimit());

			result = true;
		} catch (Exception e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void extractBugEvents(){
		Date d1 = new Date();

		int linesWithError = 0;
		Integer numberOfEventsPulled = 0;
		String errorProneProjectId = "";
		String errorProneBugNumber = "";
		int totalEventsPulledInThisRun = 0; //this is the total number of events pulled and saved in this run.
		int totalBugsWhoseEventsIsPulled = 0; //this counts the total bugs whose events is pulled and saved in this run + previous runs.
		int bugsWhoseEventsIsSavedInThisRun = 0; //this counts the bugs whose events is pulled and saved in this run.
		boolean criticalError = false;
		try {
			//1: Read all projects in the form of HashSet<id]]>:
			System.out.println("-----------------------------------");
			System.out.println("1- Reading projects into a HashMap:");
			System.out.println("Started ...");
			BufferedReader br1 = new BufferedReader(new FileReader(DATASET_DIRECTORY_GH_TSV + "\\projects_complete.tsv"));
			String s = br1.readLine(); //Skip the title line.
			HashMap<String, String> projects = new HashMap<String, String>(); //: projects and the tags available in their title/body/listOfTopLanguages.
			int i = 0;
			while((s = br1.readLine()) != null) {
				String fields[] = s.split("\t");
				if (fields.length == 6){
					String projectId = fields[0];
					String ownerAndRepoStrings = fields[2];
					projects.put(projectId, ownerAndRepoStrings);
					i++;
				}
				else
					linesWithError++;
			}
			br1.close();
			System.out.println("Number of bugs read: " + i);
			if (linesWithError > 0)
				System.out.println("Finished with " + linesWithError + " errors.");
			else
				System.out.println("Finished.");
			System.out.println("-----------------------------------");


			//2: Read all bugs that their events have been completely pulled before, in the form of HashSet<[projectId;;issueNumber]>:
			System.out.println("-----------------------------------");
			System.out.println("2- Reading bugs_complete_EventsHaveBeenPulled.tsv into a HashSet:");
			System.out.println("Started ...");
			BufferedReader br2 = new BufferedReader(new FileReader(DATASET_DIRECTORY_GH_TSV + "\\bugsWhoseEventsHaveBeenPulled.tsv"));
			HashSet<String> bugs_previouslyPulled = new HashSet<String>();
			s = br2.readLine(); //Skip the title line.
			i = 0;
			linesWithError = 0;
			while((s = br2.readLine()) != null) {
				String[] fields = s.split("\t");
				if (fields.length == 11){
					String projectId = fields[1];
					String bugNumber = fields[2];

					bugs_previouslyPulled.add(projectId + ";;" + bugNumber);
					i++;
					if (i % 10000 == 0)
						System.out.println(i);
				}
				else
					linesWithError++;
			}
			br2.close();
			System.out.println("Number of bugsPreviouslyHaveBeenRead: " + i);
			if (linesWithError > 0)
				System.out.println("Finished with " + linesWithError + " errors.");
			else
				System.out.println("Finished.");
			System.out.println("-----------------------------------");


			//3: Read the bugs (one by one) and pull events of each one, and save the events (also save the bug line in bugs_complete_EventsHaveBeenPulled.tsv):
			System.out.println("-----------------------------------");
			System.out.println("3- Reading bugs and then pulling the events of each bug:");
			System.out.println("Started ...");

			GitHubClient client = null;
			IssueService is = null;

			//Authentication:
			boolean authenticated;
			try{
				client = new GitHubClient();
				//OAuth2 token authentication
				client.setUserAgent("TaskAssignment/software-expertise");
				client.setHeaderAccept("application/vnd.github.v3+json");
				client.setOAuth2Token("19e383c976807df0359e36ba05938027e4a20c45");
				//prepare the issue service:
				is = new IssueService(client);
				authenticated = true;
			} catch (Exception e) {
				authenticated = false;
				e.printStackTrace();
			}

			if (authenticated){
				totalBugsWhoseEventsIsPulled = 0; //i counts the total bugs whose events is pulled and saved in this run + previous runs.
				bugsWhoseEventsIsSavedInThisRun = 0; //j counts the bugs whose events is pulled and saved in this run.

				FileWriter bugsWhoseEventsHaveBeenPulled_writer = new FileWriter(DATASET_DIRECTORY_GH_TSV+"\\bugsWhoseEventsHaveBeenPulled.tsv", true);
				FileWriter bugEvents_writer = new FileWriter(DATASET_DIRECTORY_GH_TSV+"\\bugEvents.tsv", true);

				BufferedReader br3 = new BufferedReader(new FileReader(DATASET_DIRECTORY_GH_TSV + "\\bugs_complete.tsv"));
				s = br3.readLine(); //Skip the title line.
				linesWithError = 0;
				Date checkPoint1 = new Date();
				while((s = br3.readLine()) != null) {
					String[] fields = s.split("\t");
					if (fields.length == 11){
						String projectId = fields[1];
						String bugNumber = fields[2];
						if (!bugs_previouslyPulled.contains(projectId + ";;" + bugNumber)){//: if we haven't read this bug before:
							String ownerAndrepo = projects.get(projectId);
							String[] ownerAndRepoStrings = ownerAndrepo.split("/");
							String owner = ownerAndRepoStrings[0].toLowerCase();
							String repo = ownerAndRepoStrings[1].toLowerCase();

							if (downloadAndSaveBugEventsInFile(client, is, projectId, owner, repo, bugNumber, bugEvents_writer, numberOfEventsPulled)){
								bugsWhoseEventsHaveBeenPulled_writer.append(s + "\n");
								bugsWhoseEventsHaveBeenPulled_writer.flush();
								bugsWhoseEventsIsSavedInThisRun++;
								totalEventsPulledInThisRun = totalEventsPulledInThisRun + numberOfEventsPulled;
							}
							else{
								criticalError = true;
								errorProneProjectId = projectId;
								errorProneBugNumber = bugNumber;
								break;
							}
						}
						totalBugsWhoseEventsIsPulled++;
//						if (totalBugsWhoseEventsIsPulled % 10000 == 0)
//							System.out.println(totalBugsWhoseEventsIsPulled);
						if ((bugsWhoseEventsIsSavedInThisRun+1) % 4900 == 0){
//						if ((bugsWhoseEventsIsSavedInThisRun+1) % 3000 == 0){//set to a lower amount.
//							break;
							Date checkPoint2 = new Date();
							float seconds = (float)(checkPoint2.getTime()-checkPoint1.getTime())/1000;
							System.out.println("Events of 4900 more bugs were extracted and saved in " + seconds + "seconds");
//							System.out.println("Events of 2999 more bugs were extracted and saved in " + seconds + "seconds");
							int delay;
//							if (seconds <= 3600) //:this is common. It usually takes ~700 seconds.
//								delay = 1000*Math.min(3600-(int)seconds+300, 3600);
//							else
//								delay = 2000000; //:this is rare.
							delay = 3600000; //set to a higher delay
							System.out.println("Now wait for " + delay/1000 + " seconds ...");
							Thread.sleep(delay);
							checkPoint1 = new Date();
							System.out.println("Now continue calling Github API's 4900 more times ...");
						}
					}
					else
						linesWithError++;
				}
				br3.close();
				bugsWhoseEventsHaveBeenPulled_writer.close();
				bugEvents_writer.close();
			}
			else
				System.out.println("Authentication error!");

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Number of bugs whose events are pulled in this run: " + bugsWhoseEventsIsSavedInThisRun);
		System.out.println("Total number of bugs whose events are pulled up to now: " + totalBugsWhoseEventsIsPulled);
		if (criticalError || linesWithError>0){
			System.out.println("Finished with error(s)!");
			if (criticalError)
				System.out.println("Check the contents of \"bugEvents.tsv\" and \"bugsWhoseEventsHaveBeenPulled.tsv\". There may be events that are partially saved for the new bug.");
			if (linesWithError > 0)
				System.out.println("There are " + linesWithError + " errors related to the length of the bugs.");
			if (!errorProneProjectId.equals("") || !errorProneBugNumber.equals(""))
				System.out.println("Error pulling events of projectId:\"" + errorProneProjectId + "\", bugNumber:\"" + errorProneBugNumber + "\".");
			if (numberOfEventsPulled > 0)
				System.out.println("There are " + numberOfEventsPulled + " events that are pulled incompletely and need to be pulled again.");
		}
		else
			System.out.println("Finished.");
		System.out.println("-----------------------------------");

		Date d2 = new Date();
		System.out.println("Total time: " + (float)(d2.getTime()-d1.getTime())/1000  + " seconds.");
		System.out.println("Finished.");
		System.out.println("-------------------------");
	}
	//------------------------------------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------------------------------------
	public static void main(String[] args) {
		extractBugEvents();

//		try{
//			GitHubClient client = new GitHubClient();
//			//OAuth2 token authentication
//			client.setUserAgent("TaskAssignment/software-expertise");
//			client.setHeaderAccept("application/vnd.github.v3+json");
//			client.setOAuth2Token("19e383c976807df0359e36ba05938027e4a20c45");
//			//prepare the issue service:
//			IssueService is = new IssueService(client);
//			System.out.println(client.getRemainingRequests());
//			System.out.println(client.getRequestLimit());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}



//		Date checkPoint1 = new Date();
//		for (int k=0; k<20; k++){
//			System.out.println("Step: " + k);
//			int x = 1;
//			for (int i=0; i<1000000; i++)
//				for (int j=0; j<10000; j++)
//					x = x + i*2*j;
//			System.out.println("                            " + x);
//
//			Date checkPoint2 = new Date();
//			float seconds = (float)(checkPoint2.getTime()-checkPoint1.getTime())/1000;
//			System.out.println("Events of 4990 more bugs were extracted and saved in " + seconds + "seconds");
//			int delay;
//			if (seconds <= 10)
//				delay = 10-(int)seconds+2;
//			else
//				delay = 10;
//			System.out.println("Now wait for " + delay);
//			try {
//				Thread.sleep(delay);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			checkPoint1 = new Date();
//			System.out.println("Now continue calling Github API's ...");
//			System.out.println();
//		}





	}

}
