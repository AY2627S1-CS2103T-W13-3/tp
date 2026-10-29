---
layout: page
title: PolicyHarbor
homepage: true
description: A command-first desktop workspace for independent insurance agents in Singapore. Keep client details and the next conversation together.
---
{%- assign release_url = 'https://github.com/AY2627S1-CS2103T-W13-3/tp/releases/tag/v1.5' -%}
{%- assign user_guide = '/UserGuide.html' | relative_url -%}
<!--
  Design: a calm working desk, with the real product as its proof. Green, ivory,
  editorial serif headings and the application's restrained sans-serif body.
  Visitor path: understand the workspace, see it, try a short workflow, download.
  Release contract: published v1.5 at tag v1.5, commit cb060cf3.
  Download actions lead to that versioned GitHub release.
  Screenshot: one live JavaFX scene from a packaged dd3b0020 build, captured
  2026-10-29 with fictional data. No compositing or interface edits.
-->
<div class="ph-home">
  <a class="ph-skip" href="#product">Skip to product overview</a>

  <section class="ph-hero" id="product" aria-labelledby="ph-title">
    <div class="ph-hero-copy">
      <h1 id="ph-title">Every client remembered.<br><span>Every next step in sight.</span></h1>
      <p class="ph-tagline">Clients. Follow-ups. Made simple.</p>
    </div>
    <div class="ph-hero-intro">
      <p>A clearer workspace for independent insurance agents in Singapore. Keep client details and the next conversation together, with typed commands and a graphical overview.</p>
      <div class="ph-actions">
        <a class="ph-button" href="{{ release_url }}">Download PolicyHarbor v1.5 <span aria-hidden="true">↗</span></a>
        <a class="ph-text-link" href="{{ user_guide }}">Explore the User Guide <span aria-hidden="true">→</span></a>
      </div>
      <p class="ph-requirement">PolicyHarbor v1.5 <span aria-hidden="true">·</span> Desktop app <span aria-hidden="true">·</span> Java 25</p>
    </div>
    <figure class="ph-product-shot">
      <a href="{{ '/images/Ui.png' | relative_url }}" aria-label="Open the full-size PolicyHarbor v1.5 screenshot">
        <img src="{{ '/images/Ui.png' | relative_url }}" width="2880" height="1722" fetchpriority="high"
             alt="PolicyHarbor v1.5 showing ten fictional Singapore clients, Rachel Lim's contact profile, and overdue, due-today and upcoming follow-up statuses. Permanent Client IDs stay visible beside each name." />
      </a>
      <figcaption><span>A real PolicyHarbor workspace. Fictional clients, everyday conversations.</span><a href="{{ '/images/Ui.png' | relative_url }}">View screenshot at full size <span aria-hidden="true">↗</span></a></figcaption>
    </figure>
  </section>

  <section class="ph-context ph-section" aria-labelledby="ph-context-title">
    <h2 id="ph-context-title">Keep the person<br>and the next step together.</h2>
    <div class="ph-context-copy">
      <p>Before the next call, you need the right contact details, the context of the conversation and a clear next action.</p>
      <p>PolicyHarbor brings those essentials into one desktop workspace. Look up a client, review their profile and see which conversations need your attention.</p>
    </div>
  </section>

  <section class="ph-section ph-capabilities" aria-labelledby="ph-capabilities-title">
    <div class="ph-section-heading">
      <h2 id="ph-capabilities-title">The essentials, within reach.</h2>
      <p>Focused tools for your client portfolio. Ready for your next conversation.</p>
    </div>
    <div class="ph-feature-grid">
      <article class="ph-feature">
        <h3>A complete client profile</h3>
        <p>Review phone, email, address, tags and the current pending action together. Open a profile with <code>view</code> and its permanent Client ID.</p>
      </article>
      <article class="ph-feature">
        <h3>A clear view of what is due</h3>
        <p>See follow-ups grouped as overdue, due today and upcoming. The global summary stays visible while you search the client table.</p>
      </article>
      <article class="ph-feature">
        <h3>Find the right client</h3>
        <p>Search a name fragment, part of a phone number or an exact tag. Review the matches, then open the right profile by Client ID.</p>
      </article>
      <article class="ph-feature">
        <h3>A client identity that stays put</h3>
        <p>Permanent Client IDs stay unchanged when you search, edit details or reorder follow-ups. Correct a contact with <code>edit</code> while retaining their ID and pending action.</p>
      </article>
      <article class="ph-feature">
        <h3>One clear next action</h3>
        <p>Record one dated action per client, replace it when plans change and clear it when complete. Keep the current action beside their contact details.</p>
      </article>
      <article class="ph-feature">
        <h3>Your workspace, saved locally</h3>
        <p>Client records and follow-ups are saved to a local JSON file and reloaded when you return. If saved client data cannot be loaded, changes are blocked to protect that file.</p>
      </article>
    </div>
  </section>

  <section class="ph-workflow ph-section" aria-labelledby="ph-workflow-title">
    <div class="ph-section-heading">
      <h2 id="ph-workflow-title">From a new contact<br>to the next conversation.</h2>
      <p>Try these commands in a fresh workspace, one line at a time. Rachel receives Client ID 1 in this example.</p>
    </div>
    <ol class="ph-steps">
      <li>
        <div class="ph-step-copy"><h3>Add a client</h3><p>Capture Rachel's contact details and a useful tag.</p></div>
        <pre><code>add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 123 t/health</code></pre>
      </li>
      <li>
        <div class="ph-step-copy"><h3>Find and review</h3><p>Search part of her name, then open her profile using Client ID 1.</p></div>
        <pre><code>find n/rac
view 1</code></pre>
      </li>
      <li>
        <div class="ph-step-copy"><h3>Plan the next conversation</h3><p>Record the next action, then review the pending list.</p></div>
        <pre><code>followup 1 d/2026-11-12 m/Discuss renewal options
followups</code></pre>
      </li>
    </ol>
    <p class="ph-workflow-note">The date is illustrative; choose the actual due date, including a past date for overdue work. For an existing portfolio, read Rachel's permanent Client ID from the table and use it in place of <code>1</code>.</p>
  </section>

  <section class="ph-perspective ph-section" aria-labelledby="ph-perspective-title">
    <h2 id="ph-perspective-title">The speed of commands.<br>The clarity of a dashboard.</h2>
    <div>
      <p>Keep your hands on the keyboard and your next actions in view. Use Enter or Run to submit commands; rejected input stays ready to correct. The table, profile and global summary share one workspace.</p>
      <p>Records live on your computer. There is no online account to set up, and the app manages one current pending action per client.</p>
      <a class="ph-text-link" href="{{ user_guide }}">See the complete feature guide <span aria-hidden="true">→</span></a>
    </div>
  </section>

  <section class="ph-setup ph-section" id="get-started" aria-labelledby="ph-setup-title">
    <div class="ph-section-heading">
      <h2 id="ph-setup-title">A small setup.<br>A workspace of your own.</h2>
      <p>PolicyHarbor runs as a desktop application on Java 25.</p>
    </div>
    <ol class="ph-install">
      <li><h3>Install Java 25</h3><p>On macOS, use the <a href="https://se-education.org/guides/tutorials/javaInstallationMac.html">prescribed Zulu JDK+FX distribution</a>. Check your installation with <code>java -version</code>.</p></li>
      <li><h3>Download the JAR</h3><p>Download <code>PolicyHarbor.jar</code> from the <a href="{{ release_url }}">official v1.5 release</a> and place it in a writable folder for your client data.</p></li>
      <li><h3>Open your workspace</h3><p>Open a terminal in that folder and run:</p><pre><code>java -jar PolicyHarbor.jar</code></pre></li>
    </ol>
    <a class="ph-text-link" href="{{ user_guide }}#quick-start">Read the full installation instructions <span aria-hidden="true">→</span></a>
  </section>

  <section class="ph-closing" aria-labelledby="ph-closing-title">
    <h2 id="ph-closing-title">Stay close to your clients.<br>Stay ahead of your next step.</h2>
    <div class="ph-actions">
      <a class="ph-button ph-button-light" href="{{ release_url }}">Download PolicyHarbor v1.5 <span aria-hidden="true">↗</span></a>
      <a class="ph-text-link" href="{{ user_guide }}">Read the User Guide <span aria-hidden="true">→</span></a>
    </div>
    <p>Clients. Follow-ups. Made simple.</p>
  </section>

  <footer class="ph-footer">
    <div class="ph-footer-top">
      <a class="ph-wordmark" href="{{ '/' | relative_url }}">PolicyHarbor</a>
      <nav aria-label="Product and project resources">
        <a href="{{ user_guide }}">User Guide</a>
        <a href="{{ '/DeveloperGuide.html' | relative_url }}">Developer Guide</a>
        <a href="{{ '/AboutUs.html' | relative_url }}">About Us</a>
        <a href="https://github.com/AY2627S1-CS2103T-W13-3/tp">GitHub</a>
        <a href="https://github.com/AY2627S1-CS2103T-W13-3/tp/issues">Issue tracker</a>
      </nav>
    </div>
    <p>Based on <a href="https://github.com/se-edu/addressbook-level3">AddressBook Level 3</a> by <a href="https://se-education.org">SE-EDU</a>. Distributed under the <a href="https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/LICENSE">MIT License</a>. <a href="{{ '/DeveloperGuide.html' | relative_url }}#acknowledgements">Acknowledgements</a>.</p>
    <p>Headings use <a href="{{ '/assets/landing/OFL.txt' | relative_url }}">Newsreader, licensed under the SIL Open Font License</a>.</p>
  </footer>
</div>
