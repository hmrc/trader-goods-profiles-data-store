# Checklist for developers before reviewing
- [ ] I have merged in main
- [ ] I have run  `sbt prePR` to run all tests, format and check the code coverage
- [ ] I have written unit tests to cover any code that I wrote, where necessary
- [ ] I have ran the microservices with the latest SM2 config
- [ ] I have checked all the Acceptance Criteria pass
- [ ] I have run the pipeline journey tests
- [ ] I have run the local journey tests
- [ ] I have moved the ticket to Review in Jira
- [ ] I have left a message in tgp-prs on slack to alert other Devs that this ticket is ready for review

# Checklist for developers before merging
- [ ] The PR has been approved by 2 other developers
- [ ] I have merged in main
- [ ] I have run  `sbt preMerge' to run all tests, check formatting and code coverage
- [ ] I have ran the microservices with the latest SM2 config
- [ ] I have checked all the Acceptance Criteria pass
- [ ] I have run the pipeline journey tests
- [ ] I have run the local journey tests
- [ ] I have spoken to the QAs about any changes that may need to be made to the journey tests OR no changes to the journey tests are necessary

# Reminders for developers after merging
1. Wait for pipeline to pass
2. Move ticket to QA column in Jira
3. Leave message in tgp-qa on slack to alert QAs that this ticket is ready for testing
