## Brief

<!-- Which issue does this PR fix? Ideally, create an issue if there was none, so the problem in question is well stated. -->

## QA plan

<!-- Please state a reproducible plan to prove this PR works. Attach screenshots, gifs, etc. if needed. Occasionally, sufficient test coverage removes the need for QAing. -->

## Author checklist

<!-- Please, before publicizing your PR, open it as a "WIP PR", and then review it using the following. -->

* [ ] I have QAed the functionality
* [ ] The PR has a reasonably reviewable size and a meaningful commit history
* [ ] I have run the [branch formatter](https://github.com/nedap/formatting-stack/blob/332a419034ab46fad526a5592f4257353bd695b6/src/formatting_stack/branch_formatter.clj) and observed no new/significative warnings
* [ ] The build passes
* [ ] I have self-reviewed the PR prior to assignment
* Additionally, I have code-reviewed iteratively the PR considering the following aspects in isolation:
  * [ ] Correctness
  * [ ] Robustness (red paths, failure handling etc)
  * [ ] Modular design
  * [ ] Test coverage
  * [ ] Spec coverage
  * [ ] Documentation
  * [ ] Security
  * [ ] Performance
  * [ ] Breaking API changes

## Reviewer checklist

* [ ] I have checked out this branch and reviewed it locally, running it
* [ ] I have QAed the functionality
* [ ] I have reviewed the PR
* Additionally, I have code-reviewed iteratively the PR considering the following aspects in isolation:
  * [ ] Correctness
  * [ ] Robustness (red paths, failure handling etc)
  * [ ] Modular design
  * [ ] Test coverage
  * [ ] Spec coverage
  * [ ] Documentation
  * [ ] Security
  * [ ] Performance
  * [ ] Breaking API changes

<!-- The following is a template for PRs automatically created with `com.nedap.staffing-solutions/ci.release-workflow`. -->
<!-- Please uncomment it when adequate, deleting the rest of template. -->
<!--

Delivers: <!-- (place here links to the included PRs/commits) -->

## Release checklist (author)

* [ ] All PRs / relevant commits since the previous release are listed in this PR's description 
* [ ] The new proposed version follows semver 
* [ ] The build passes
* [ ] New features are (briefly) reflected in the README

## Release checklist (reviewer)

* [ ] All PRs / relevant commits since the previous release are listed in this PR's description 
* [ ] The new proposed version follows semver 
* [ ] New features are (briefly) reflected in the README

-->
