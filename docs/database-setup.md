# Connect the database when you have access

The Android app already has local storage and the existing website's public Supabase configuration. Offline Stimulate does not require a database account.

## Access to the existing Supabase project

Ask its owner to add **`brainstem://auth/callback`** under Authentication → URL Configuration → Redirect URLs. Keep the existing Site URL and website redirects. Google must also be enabled under Authentication → Sign In / Providers. The Android app uses browser OAuth with PKCE; the callback returns to the installed app.

The existing project must expose `public.app_profile` and the private `profile-photos` bucket to each signed-in account through row-level policies. The app reads profiles using the user's access token, writes the fields in `Auth.saveProfile`, and stores JPEGs at `<auth-user-id>/<profile-id>.jpg`. Ask the owner to inspect the existing schema and policies; do not run the new-project SQL against a working shared database.

Use two test accounts to check that account A cannot read or change account B's profiles or photos. Then verify email login, refresh after reopening, Google return, profile creation/editing, photo upload/removal, and sign out. These real-account checks have not been run by this project.

## A new Supabase project that you own

1. Create a project, then run [new-project.sql](new-project.sql) once in its SQL Editor. It creates the profile table, account ownership policies, and a private JPEG bucket. It intentionally fails if those objects already exist. This starter is based on the Android REST contract, not an export of the original database. It has not been executed against a Supabase project here.
2. In `app/src/main/assets/backend.json`, replace `url` with your project URL and `key` with its **publishable** key. Keep `redirect` as `brainstem://auth/callback`. Never put a service-role/secret key or database password in the Android app.
3. Set `emailRedirect` to a confirmation/password-reset web page that you control and that uses this new project's Supabase configuration. Allow that HTTPS URL in Supabase. The original website handles the original project; it cannot be assumed to process a different project's reset tokens.
4. Enable email sign-in and configure the project's email sender. For Google, follow Supabase's provider setup: the Google Cloud authorized redirect URI is your Supabase `https://<project-ref>.supabase.co/auth/v1/callback`; the **application** redirect allowlist contains `brainstem://auth/callback`. These are two different redirects.
5. Rebuild and install the APK. Existing users and data are not copied to the new project automatically. Use the two-account checks above before connecting real profiles.

This schema supports accounts, demographic profiles, and profile photos. Session history, custom programs, queue, favorites, and simulator settings are local to each installation, matching the inspected module's local storage behavior. Adding their cloud synchronization would require a separate API and conflict-resolution design.

References: [Supabase redirects](https://supabase.com/docs/guides/auth/redirect-urls), [Google provider setup](https://supabase.com/docs/guides/auth/social-sign-in/auth-google), [row-level security](https://supabase.com/docs/guides/database/postgres/row-level-security), and [private Storage access policies](https://supabase.com/docs/guides/storage/security/access-control).
