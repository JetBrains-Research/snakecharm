Feature: Inspection: unresolved rule name if 'use' section declaration

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

    use rule other_NAME2 as name2_other with:
      input: "data.txt"

    use rule NAME3 from MODULE2 with:
      threads: 7
    """
    And SmkUnresolvedReferenceInUseSectionInspection inspection is enabled
    Then I expect inspection weak warning on <NAME1> with message
    """
    Resolved rule may not contains in imported modules
    """
    Then I expect inspection weak warning on <other_NAME2> with message
    """
    Resolved rule may not contains in imported modules
    """
    Then I expect inspection weak warning on <NAME3> with message
    """
    Resolved rule may not contains in imported modules
    """
    When I check highlighting weak warnings

  Scenario: Generic error if there are no imports or appropriate wildcard pattern
    Given a snakemake project
    And a file "boo.smk" with text
    """
    rule z:
      input: "file.csv"
    """
    And I open a file "foo.smk" with text
    """
    module MODULE:
      snakefile: "boo.smk"

    use rule NAME1 from MODULE with:
      threads: 5

    use rule * from MODULE as other_*

    use rule other_NAME2 as name2_other with:
      input: "data.txt"

    module MODULE2:
      snakefile: "https://github.com/useful_smk_diles/file.smk"

    use rule * from MODULE2 as not_*

    use rule NAME_not_NAME as with:
      threads: 7

    """
    And SmkUnresolvedReferenceInUseSectionInspection inspection is enabled
    Then I expect inspection error on <NAME1> with message
    """
    Cannot find rule with name 'NAME1'
    """
    Then I expect inspection error on <other_NAME2> with message
    """
    Cannot find rule with name 'other_NAME2'
    """
    Then I expect inspection error on <NAME_not_NAME> with message
    """
    Cannot find rule with name 'NAME_not_NAME'
    """
    When I check highlighting errors