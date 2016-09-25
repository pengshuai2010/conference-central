package com.google.devrel.training.conference.spi;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.List;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Profile;
import com.google.devrel.training.conference.form.ConferenceForm;
import com.google.devrel.training.conference.form.ConferenceQueryForm;
import com.google.devrel.training.conference.form.ProfileForm;
import com.google.devrel.training.conference.form.ProfileForm.TeeShirtSize;
import com.google.devrel.training.conference.service.OfyService;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

/**
 * Defines conference APIs.
 */
@Api(name = "conference", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = {
        Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, description = "API for the Conference Central Backend application.")
public class ConferenceApi {

    /*
     * Get the display name from the user's email. For example, if the email is
     * lemoncake@example.com, then the display name becomes "lemoncake."
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @param profileForm
     *            A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException
     *             when the User object is null.
     */

    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that
    // conforms to the fields defined in ProfileForm

    // TODO 1 Pass the ProfileForm parameter
    // TODO 2 Pass the User parameter
    public Profile saveProfile(final User user, ProfileForm profileForm) throws UnauthorizedException {
        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) 
        	throw new UnauthorizedException("user is not logged in");
        
        TeeShirtSize teeShirtSize = profileForm.getTeeShirtSize();
        String displayName = profileForm.getDisplayName();
		String userId = user.getUserId();
		String mainEmail = user.getEmail();

        Profile profile = getProfile(user);
        if (profile != null) {
			profile.update(displayName, teeShirtSize);
        } else {
			if (teeShirtSize == null)
				teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
			if (displayName == null)
				displayName = extractDefaultDisplayNameFromEmail(mainEmail);
        	profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        }
        // Save the Profile entity in the datastore
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException
     *             when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // load the Profile Entity
        String userId = user.getUserId(); // TODO
        Key<Profile> key = Key.create(Profile.class, userId); // TODO
        Profile profile = (Profile) ofy().load().key(key).now(); // TODO load the Profile entity
        return profile;
    }
    
    /**
     * Gets the Profile entity for the current user
     * or creates it if it doesn't exist
     * @param user
     * @return user's Profile
     */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(user.getUserId(),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
        }
        return profile;
    }

/**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO (Lesson 4)
        // Get the userId of the logged in User
        String userId = user.getUserId();

        // TODO (Lesson 4)
        // Get the key for the User's Profile
        Key<Profile> profileKey =  Key.create(Profile.class, userId);

        // TODO (Lesson 4)
        // Allocate a key for the conference -- let App Engine allocate the ID
        // Don't forget to include the parent Profile in the allocated ID
        final Key<Conference> conferenceKey = OfyService.factory().allocateId(profileKey, Conference.class);

        // TODO (Lesson 4)
        // Get the Conference Id from the Key
        final long conferenceId = conferenceKey.getId();

        // TODO (Lesson 4)
        // Get the existing Profile entity for the current user if there is one
        // Otherwise create a new Profile entity with default values
        Profile profile = getProfileFromUser(user);

        // TODO (Lesson 4)
        // Create a new Conference Entity, specifying the user's Profile entity
        // as the parent of the conference
        Conference conference = new Conference(conferenceId, userId, conferenceForm);

        // TODO (Lesson 4)
        // Save Conference and Profile Entities
        ofy().save().entities(profile, conference).now();
         

         return conference;
    }
    
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
        Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
        List<Conference> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
        for (Conference conference : conferenceIterable) {
            organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerUserId()));
            result.add(conference);
        }
        // To avoid separate datastore gets for each Conference, pre-fetch the Profiles.
        ofy().load().keys(organizersKeyList);
        return result;
    }
    
    @ApiMethod(
    		name = "getConferencesCreated",
    		path = "getConferencesCreated",
    		httpMethod = HttpMethod.POST
    		)
    public List<Conference> getConferencesCreated(final User user) throws UnauthorizedException {
    	if (user == null)
            throw new UnauthorizedException("Authorization required");
    	Query<Conference> query = ofy().load().type(Conference.class).ancestor(Key.create(Profile.class, user.getUserId()));
    	return query.list();
    }
    
    @ApiMethod(
    		name = "filterPlayground",
    		path = "filterPlayground",
    		httpMethod = HttpMethod.POST
    		)
    public List<Conference> filterPlayground() {
//    	Query<Conference> query = ofy().load().type(Conference.class).order("name").filter("city =", "London").filter("topics =", "Medical Innovations");
//    	Query<Conference> query = ofy().load().type(Conference.class).order("name").filter("city =", "London").filter("month =", 6);
    	Query<Conference> query = ofy().load().
    			type(Conference.class).
    			order("name").
    			filter("city =", "London").
    			filter("topics =", "Medical Innovations").
    			filter("month =", 6).
    			filter("maxAttendees >", 500);
    	// note that query if immutable, so it's no use only do query.filter(...);
    	// you must write query = query.filter(...);
    	return query.list();
    }
}
