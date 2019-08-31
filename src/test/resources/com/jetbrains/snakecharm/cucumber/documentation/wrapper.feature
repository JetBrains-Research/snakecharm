Feature: Documentation for wrapper section arguments.
  Please check testData/wrapper/storage_data.txt to see/modify what data is 'cached' for these tests

  Scenario Outline: documentation for cached wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.36.0/bio/samtools/sort"
    """
    And I prepare wrapper storage
    When I put the caret after wrapper: "
    Then I invoke quick documentation popup
    Then Documentation text should be equal to
    """
    <html><body><div class='definition'><pre>Snakemake Wrapper</pre></div><div class='content'>0.36.0/bio/samtools/sort</div><p><table class='sections'><tr><td valign='top' class='section'><p>wrapper.py</td><td valign='top'><p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.36.0/bio/samtools/sort/wrapper.py">https://bitbucket.org/snakemake/snakemake-wrappers/src/0.36.0/bio/samtools/sort/wrapper.py</a></td><tr><td valign='top' class='section'><p>description</td><td valign='top'><p>Sort:&nbsp;bam:&nbsp;file:&nbsp;with:&nbsp;samtools.<p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.36.0/bio/samtools/sort/meta.yaml">(meta.yaml)</a></table><tr><td valign='top' class='section'><p>dependencies</td><td valign='top'><p>-:&nbsp;samtools:&nbsp;==1.9<p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.36.0/bio/samtools/sort/environment.yaml">(environment.yaml)</a></table><tr><td valign='top' class='section'><p>example</td><td valign='top'><p>rule:&nbsp;samtools_sort:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;input:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"mapped/{sample}.bam"<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;output:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"mapped/{sample}.sorted.bam"<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;params:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"-m:&nbsp;4G"<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;threads::&nbsp;:&nbsp;#:&nbsp;Samtools:&nbsp;takes:&nbsp;additional:&nbsp;threads:&nbsp;through:&nbsp;its:&nbsp;option:&nbsp;-@<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;8:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;#:&nbsp;This:&nbsp;value:&nbsp;-:&nbsp;1:&nbsp;will:&nbsp;be:&nbsp;sent:&nbsp;to:&nbsp;-@.<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;wrapper:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"master/bio/samtools/sort"<p><p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.36.0/bio/samtools/sort/test/Snakefile">(test/Snakefile)</a></table></table></body></html>
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |

  Scenario Outline: documentation for older than cached wrapper
    Given a snakemake project
    Given I open a file "foo.smk" with text
    """
    <rule_like> foo:
      wrapper: "0.33.0/bio/samtools/sort"
    """
    And I prepare wrapper storage
    When I put the caret after wrapper: "
    Then I invoke quick documentation popup
    Then Documentation text should be equal to
    """
    <html><body><div class='definition'><pre>Snakemake Wrapper</pre></div><div class='content'>0.33.0/bio/samtools/sort</div><p><table class='sections'><tr><td valign='top' class='section'><p>wrapper.py</td><td valign='top'><p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.33.0/bio/samtools/sort/wrapper.py">https://bitbucket.org/snakemake/snakemake-wrappers/src/0.33.0/bio/samtools/sort/wrapper.py</a></td><tr><td valign='top' class='section'><p>description</td><td valign='top'><p>Sort:&nbsp;bam:&nbsp;file:&nbsp;with:&nbsp;samtools.<p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.33.0/bio/samtools/sort/meta.yaml">(meta.yaml)</a></table><tr><td valign='top' class='section'><p>dependencies</td><td valign='top'><p>-:&nbsp;samtools:&nbsp;==1.9<p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.33.0/bio/samtools/sort/environment.yaml">(environment.yaml)</a></table><tr><td valign='top' class='section'><p>example</td><td valign='top'><p>rule:&nbsp;samtools_sort:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;input:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"mapped/{sample}.bam"<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;output:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"mapped/{sample}.sorted.bam"<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;params:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"-m:&nbsp;4G"<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;threads::&nbsp;:&nbsp;#:&nbsp;Samtools:&nbsp;takes:&nbsp;additional:&nbsp;threads:&nbsp;through:&nbsp;its:&nbsp;option:&nbsp;-@<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;8:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;#:&nbsp;This:&nbsp;value:&nbsp;-:&nbsp;1:&nbsp;will:&nbsp;be:&nbsp;sent:&nbsp;to:&nbsp;-@.<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;wrapper:<p>:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;:&nbsp;"master/bio/samtools/sort"<p><p><a href="https://bitbucket.org/snakemake/snakemake-wrappers/src/0.33.0/bio/samtools/sort/test/Snakefile">(test/Snakefile)</a></table></table></body></html>
    """
    Examples:
      | rule_like  |
      | rule       |
      | checkpoint |