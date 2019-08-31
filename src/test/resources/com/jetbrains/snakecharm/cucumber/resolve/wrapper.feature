Feature: Resolve for wrapper section arguments.
  Please check testData/wrapper/storage_data.txt to see/modify what data is 'cached' for these tests

  Scenario Outline: Resolve an existing link
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.36.0/bio/samtools/sort"
    """
    And I prepare wrapper storage
    When I put the caret at bio
    Then reference should resolve to file "wrapper.py" at path "bitbucket.org/snakemake/snakemake-wrappers/raw/0.36.0/bio/samtools/sort/wrapper.py"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: resolve a link to a non-existent file
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.36.0/bio/samtools/srt"
    """
    And I prepare wrapper storage
    When I put the caret at bio
    Then reference should not resolve
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: resolve a link to a file with a tag older than cached
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.33.0/bio/samtools/sort"
    """
    And I prepare wrapper storage
    When I put the caret at bio
    Then reference should resolve to file "wrapper.py" at path "bitbucket.org/snakemake/snakemake-wrappers/raw/0.33.0/bio/samtools/sort/wrapper.py"
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |