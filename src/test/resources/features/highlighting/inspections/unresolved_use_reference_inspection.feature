Feature: Inspection: unresolved rule name in 'use' section declaration or after 'rules.<>' keyword

  Scenario: Weak warnings if there are appropriate pattern or module import
    Given a snakemake project
    And a file "boo.smk" with text
    """
    module MODULE2:
      snakefile: "https://github.com/useful_smk_diles/file2.smk"
    """
    And I open a file "foo.smk" with text
    """
    include: "boo.smk"

    module MODULE:
      snakefile: "https://github.com/useful_smk_diles/file.smk"

    use rule NAME1 from MODULE with:
      threads: 5

    use rule * from MODULE as other_*

    use rule * from MODULE as a*b*cde
    rules.aXXXbXXXcde

    use rule * from MODULE as a*b*cpq
    rules.aXXXbXXXcpq

    use rule other_NAME2 as name2_other with:
      input: "data.txt"

    use rule NAME3 from MODULE2 with:
      log: rules.other_rule_name.log
      threads: 7
    """
    And SmkUnresolvedImportedRuleNameInspection inspection is enabled
    Then I expect inspection weak warning on <NAME1> with message
    """
    Cannot check rule names imported from remote modules
    """
    Then I expect inspection weak warning on <other_NAME2> with message
    """
    Cannot check rule names imported from remote modules
    """
    Then I expect inspection weak warning on <NAME3> with message
    """
    Cannot check rule names imported from remote modules
    """
    Then I expect inspection weak warning on <rules.other_rule_name> with message
    """
    Cannot check rule names imported from remote modules
    """
    Then I expect inspection weak warning on <rules.aXXXbXXXcde> with message
    """
    Cannot check rule names imported from remote modules
    """
    Then I expect inspection weak warning on <rules.aXXXbXXXcpq> with message
    """
    Cannot check rule names imported from remote modules
    """
    When I check highlighting weak warnings

  Scenario: No errors if there are appropriate module import
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    module MODULE:
      snakefile: "https://github.com/useful_smk_diles/file.smk"

    use rule NAME1 from MODULE with:
      threads: 5
    """
    And PyUnresolvedReferencesInspection inspection is enabled
    Then I expect no inspection errors
    When I check highlighting errors