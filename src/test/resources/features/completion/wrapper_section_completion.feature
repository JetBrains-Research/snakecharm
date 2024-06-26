Feature: Completion for wrapper name
  Scenario Outline: Complete wrapper name for bundled wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<wrapper_repo_vers>/<short_name>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <short_name>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "<wrapper_repo_vers>/<full_name>"
    """
    Examples:
      | rule_like  | short_name         | full_name                       | wrapper_repo_vers |
      | rule       | bismark2report     | bio/bismark/bismark2report      | 0.64.0            |
      | rule       | fastqc             | bio/fastqc                      | 0.64.0            |
      | checkpoint | cairosvg           | utils/cairosvg                  | 0.64.0            |
      | checkpoint | bam2fq/interleaved | bio/samtools/bam2fq/interleaved | 0.64.0            |
      | rule       | bismark2report     | bio/bismark/bismark2report      | v1.1.1            |
      | rule       | bismark2report     | bio/bismark/bismark2report      | v7.7.7            |


  Scenario Outline: Complete wrapper name for non-standard version tag (multiple lookup items)
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<prefix>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <prefix>
    And I invoke autocompletion popup, select "<lookup>" lookup item and see a text:
    """
    <rule_like> NAME:
      wrapper: "<lookup>"
    """
    Examples:
      | rule_like  | prefix   | lookup                            |
      | rule       | 0.64.0/  | 0.64.0/bio/fastqc                 |
      | rule       | v0.64.0/ | v0.64.0/bio/fastqc                |
      | rule       | 0.64.0   | 0.64.0/bio/fastqc                 |
      | rule       | master   | master/bio/fastqc                 |
      | rule       | latest   | latest/bio/fastqc                 |
      | rule       | 0.54.3   | 0.54.3/bio/fastqc                 |
      | rule       | 0.30.10  | 0.30.10/bio/fastqc                |
      | checkpoint | 0.64.0/  | 0.64.0/bio/bismark/bismark2report |

  Scenario Outline: Complete wrapper name for non-standard version tag
    Given a snakemake project
    And I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<tag>/<short_name>"
    """
    And add snakemake framework support with wrappers loaded
    When I put the caret after <short_name>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "<tag>/<full_name>"
    """
    Examples:
      | rule_like  | tag      | short_name         | full_name                       |
      | rule       | master   | bismark2report     | bio/bismark/bismark2report      |
      | rule       | latest   | fastqc             | bio/fastqc                      |
      | checkpoint | 0.54.3   | cairosvg           | utils/cairosvg                  |
      | checkpoint | 0.30.10  | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |
      | checkpoint | v2.30.10 | bam2fq/interleaved | bio/samtools/bam2fq/interleaved |

  # TODO: configure temp wrappers dir using VFS & light fixture, e.g.
#  Scenario Outline: Complete for custom wrappers repo folder wrappers
#    Given a snakemake project
#    And with snakemake facet
#    And a file "wrappers_repo/bio/bismark/custom_wr1/wrapper.py" with text
#    """
#    """
#    And a file "wrappers_repo/bio/bismark/custom_wr1/meta.yam" with text
#    """
#    """
#    And a file "wrappers_repo/bio/bismark/custom_wr1/environment.yaml" with text
#    """
#    """
#    Given wrapper repo path "wrappers_repo"

  Scenario Outline: Complete for custom wrappers repo folder wrappers
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> NAME:
      wrapper: "<prefix>"
    """
    And add snakemake framework support without wrappers loaded
    And wrapper repo path in test dir "wrappers_storage2"
    When I put the caret after <prefix>
    And I invoke autocompletion popup and see a text:
    """
    <rule_like> NAME:
      wrapper: "<tag>/bio/bismark/custom_wr1"
    """
    Examples:
      | rule_like  | tag                                   | prefix                 |
      | rule       | master                                | master/custom_wr1      |
      | rule       | latest                                | latest/custom_wr1      |
      | checkpoint | 0.54.3                                | 0.54.3/custom_wr1      |
      | checkpoint | 0.54.3                                | 0.54.3/bismark         |
      | checkpoint | 0.30.10                               | 0.30.10/bismark/custom |
      | checkpoint | file://${TEST_DATA}/wrappers_storage2 | file://                |
      | checkpoint | file://${TEST_DATA}/wrappers_storage2 | file://custom          |
      | checkpoint | file://${TEST_DATA}/wrappers_storage2 | file://custom_wr1      |