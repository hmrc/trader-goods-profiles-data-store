# Checklist for developers before reviewing
- [ ] I have merged in main
- [ ] I have assigned the PR to me
- [ ] I ran the code with a clean compile test and ensured there are no warnings or deprecations
- [ ] I have run `sbt prePR` to run all tests, format and check the code coverage
- [ ] I have written unit tests to cover any code that I wrote, where necessary
- [ ] I have linked tickets to any TODOs I have written
- [ ] I have run the microservices with the latest SM2 config
- [ ] I have checked all the Acceptance Criteria pass
- [ ] I have run the pipeline journey tests
- [ ] I have run the local journey tests
- [ ] I have spoken to the QAs about any changes that may need to be made to the journey tests OR no changes to the journey tests are necessary
- [ ] I have moved the ticket to Review in Jira
- [ ] I have left a message in tgp-prs on slack to alert other Devs that this ticket is ready for review
- [ ] I have checked my PR is not in draft mode

# Checklist for reviewer 1
- [ ] I have added an emoji to the relevant message in tgp-prs to show I am reviewing
- [ ] I have checked out the branch
- [ ] I have run the microservices with the latest SM2 config
- [ ] I have checked all the Acceptance Criteria pass
- [ ] I reviewed the code and left comments where necessary
- [ ] I have left a message on the relevant tgp-prs message to show that I have finished reviewing

# Checklist for reviewer 2
- [ ] I have added an emoji to the relevant message in tgp-prs to show I am reviewing
- [ ] I have checked out the branch
- [ ] I have run the microservices with the latest SM2 config
- [ ] I have checked all the Acceptance Criteria pass
- [ ] I reviewed the code and left comments where necessary
- [ ] I have left a message on the relevant tgp-prs message to show that I have finished reviewing

# Checklist for developers before merging
- [ ] The PR has been approved by 2 other developers
- [ ] I have merged in main
- [ ] I have run `sbt preMerge` to run all tests, check formatting and code coverage
- [ ] I have run the microservices with the latest SM2 config
- [ ] I have checked all the Acceptance Criteria pass
- [ ] I have checked that the QAs have merged in any changes to the journey tests
- [ ] I have run the pipeline journey tests
- [ ] I have run the local journey tests

# Reminders for developers after merging
1. Wait for pipeline to pass
2. Move ticket to QA column in Jira
3. Leave message in tgp-qa on slack to alert QAs that this ticket is ready for testing
