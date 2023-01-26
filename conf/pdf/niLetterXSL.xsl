<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:fox="http://xmlgraphics.apache.org/fop/extensions"
                xmlns:scala="java:util.XSLScalaBridge">

    <xsl:include href="pdf/styles.xsl"/>

    <xsl:param name="translator" />



    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/">
        <fo:root xml:lang="en">
            <fo:layout-master-set>
                <!-- layout for the first page -->
                <fo:simple-page-master master-name="first"
                                       page-height="29.7cm"
                                       page-width="21cm"
                                       margin-top="1cm"
                                       margin-bottom="1.5cm"
                                       margin-left="3cm"
                                       margin-right="3cm">
                    <fo:region-body margin-top="1.5cm"/>
                    <fo:region-before extent="0.5cm"/>
                    <fo:region-after extent="0.5cm"/>
                </fo:simple-page-master>

                <!-- layout for the other pages -->
                <fo:simple-page-master master-name="rest"
                                       page-height="29.7cm"
                                       page-width="21cm"
                                       margin-top="1cm"
                                       margin-bottom="1.5cm"
                                       margin-left="3cm"
                                       margin-right="3cm">
                    <fo:region-body margin-top="1cm"/>
                    <fo:region-before extent="0.5cm"/>
                    <fo:region-after extent="0.5cm"/>
                </fo:simple-page-master>

                <fo:page-sequence-master master-name="basicPSM">
                    <fo:repeatable-page-master-alternatives>
                        <fo:conditional-page-master-reference master-reference="first"
                                                              page-position="first"/>
                        <fo:conditional-page-master-reference master-reference="rest"
                                                              page-position="rest"/>
                        <!-- recommended fallback procedure -->
                        <fo:conditional-page-master-reference master-reference="rest"/>
                    </fo:repeatable-page-master-alternatives>
                </fo:page-sequence-master>

            </fo:layout-master-set>
            <!-- end: defines page layout -->

            <!-- actual layout -->
            <fo:page-sequence master-reference="basicPSM">
                <!-- footer -->
                <fo:static-content role="artifact" flow-name="xsl-region-after">
                    <fo:block xsl:use-attribute-sets="footer">
                        <!-- page number -->
                        <fo:inline-container inline-progression-dimension="50%">
                            <fo:block text-align="start">
                                Page <fo:page-number/>
                            </fo:block>
                        </fo:inline-container>
                        <!-- date -->
                        <fo:inline-container inline-progression-dimension="50%">
                            <fo:block text-align="end">
                                HMRC <xsl:value-of select="root/date"/>
                            </fo:block>
                        </fo:inline-container>
                    </fo:block>


                </fo:static-content>
                <!-- body -->
                <fo:flow flow-name="xsl-region-body">

                    <!-- logo and heading -->
                    <fo:block role="Div" space-after="10px">
                        <fo:inline-container role="Div" inline-progression-dimension="22%">
                            <fo:block role="Div"
                                      border-left-style="solid"
                                      border-width="2px"
                                      border-color="#28a197"
                                      padding-start="4px">
                                <fo:wrapper role="artifact">
                                    <fo:external-graphic content-type="content-type:image/png" src="pdf/logo/hmrc-logo.jpg" fox:alt-text="HMRC" content-height="scale-to-fit"  content-width="0.8cm"/>
                                </fo:wrapper>
                                <fo:block role="P"
                                          xsl:use-attribute-sets="default-font"
                                          line-height="14pt"
                                          font-size="14pt">
                                    HM Revenue &amp; Customs
                                </fo:block>
                            </fo:block>
                        </fo:inline-container>
                        <fo:inline-container role="Div" inline-progression-dimension="78%">
                            <fo:block role="H1"
                                      text-align="end"
                                      padding-before="10px"
                                      padding-after="6px"
                                      xsl:use-attribute-sets="default-font-bold">
                                <xsl:value-of select="scala:getMessagesText($translator, 'label.your_national_insurance_letter')"/>
                            </fo:block>
                        </fo:inline-container>
                    </fo:block>


                    <!-- Addresses -->
                    <fo:block role="Div">
                        <fo:inline-container role="Div" inline-progression-dimension="63%">
                            <fo:block>
                                <fo:block role="P" xsl:use-attribute-sets="address-line">
                                    <xsl:value-of select="root/initials-name"/>
                                </fo:block>
                                <xsl:for-each select="root/address/address-line">
                                    <fo:block role="P" xsl:use-attribute-sets="address-line">
                                        <xsl:value-of select="."/>
                                    </fo:block>
                                </xsl:for-each>
                                <fo:block role="P" xsl:use-attribute-sets="address-line">
                                    <xsl:value-of select="root/postcode"/>
                                </fo:block>
                            </fo:block>
                        </fo:inline-container>
                        <fo:inline-container role="Div" inline-progression-dimension="37%">
                            <fo:block>
                                <fo:block role="P" xsl:use-attribute-sets="address-line">NIC&amp;EO</fo:block>
                                <fo:block role="P" xsl:use-attribute-sets="address-line">HMRC</fo:block>
                                <fo:block role="P" xsl:use-attribute-sets="address-line">BX9 1AN</fo:block>
                                <fo:block role="P" xsl:use-attribute-sets="p">Phone&#x9;0300 200 3500</fo:block>
                                <fo:block role="P" xsl:use-attribute-sets="p">www.gov.uk/hmrc</fo:block>
                            </fo:block>
                        </fo:inline-container>
                    </fo:block>


                    <!-- name and title -->
                    <fo:block xsl:use-attribute-sets="p">
                        <xsl:value-of select="root/full-name"/>
                    </fo:block>


                    <!-- NINO number box -->
                    <fo:block role="Div"
                              space-after="10px"
                              background-color="#DAF4F2"
                              border-style="solid"
                              border-width="1.5px"
                              border-color="#28a197">
                        <fo:block xsl:use-attribute-sets="header-small"
                                  text-align="center">
                            Your National Insurance number is
                        </fo:block>
                        <fo:block role="P"
                                  font-size="20pt"
                                  text-align="center"
                                  xsl:use-attribute-sets="default-font-bold">
                            <xsl:value-of select="root/nino"/>
                        </fo:block>
                    </fo:block>

                    <fo:block role="P"
                              space-after="10px"
                              text-align="center"
                              xsl:use-attribute-sets="default-font-and-padding-bold">
                        Keep this number in a safe place. Do not destroy this letter.
                    </fo:block>

                    <!-- about NINO information -->
                    <fo:block role="Div">
                        <fo:inline-container role="Div" inline-progression-dimension="63%">
                            <fo:block role="Div"
                                      margin-left="5px"
                                      margin-right="5px">
                                <fo:block role="H2"
                                          xsl:use-attribute-sets="header-small">
                                    About your National Insurance number
                                </fo:block>

                                <fo:block xsl:use-attribute-sets="p">
                                    Your National Insurance number is unique to you and will never change. To
                                    prevent identity fraud, do not share it with anyone who does not need it.
                                </fo:block>

                                <fo:block xsl:use-attribute-sets="p">
                                    You will need it if you:
                                </fo:block>
                                <fo:list-block xsl:use-attribute-sets="list-block">
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                start work (including part time and weekend jobs)
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                apply for a driving license
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                apply for a student loan
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                claim state benefits
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                register to vote
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                </fo:list-block>
                                <fo:block xsl:use-attribute-sets="p">
                                    It is not proof of:
                                </fo:block>
                                <fo:list-block xsl:use-attribute-sets="list-block">
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                your identity
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                your right to work in the UK
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                </fo:list-block>
                                <fo:block role="H2"
                                          xsl:use-attribute-sets="header-small">
                                    Child Trust Fund
                                </fo:block>
                                <fo:block xsl:use-attribute-sets="p">
                                    When you turn 16, take control of your Child Trust Fund. Ask your parents or
                                    guardian, for more information go to
                                    <fo:basic-link color="#531fff"
                                                   text-decoration="underline"
                                                   external-destination="www.gov.uk/child-trust-funds">
                                        www.gov.uk/child-trust-funds
                                    </fo:basic-link>
                                </fo:block>

                                <fo:block role="H2"
                                          xsl:use-attribute-sets="header-small">
                                    Welsh language
                                </fo:block>
                                <fo:block xsl:use-attribute-sets="p">
                                    To continue to receive a Welsh language service:
                                </fo:block>
                                <fo:list-block xsl:use-attribute-sets="list-block">
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                email gwasanaeth.cymraeg@hmrc.gov.uk
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                    <fo:list-item xsl:use-attribute-sets="normal-list" role="LI">
                                        <fo:list-item-label role="Lbl" end-indent="label-end()">
                                            <fo:block>
                                                <fo:inline>
                                                    <fo:wrapper role="artifact">
                                                        &#8226;
                                                    </fo:wrapper>
                                                </fo:inline>
                                            </fo:block>
                                        </fo:list-item-label>
                                        <fo:list-item-body role="LBody" start-indent="body-start()">
                                            <fo:block>
                                                phone 0300 200 1900
                                            </fo:block>
                                        </fo:list-item-body>
                                    </fo:list-item>
                                </fo:list-block>
                            </fo:block>
                        </fo:inline-container>
                        <fo:inline-container role="Div" inline-progression-dimension="37%">
                            <fo:block role="Div"
                                      background-color="#DAF4F2"
                                      margin-left="5px"
                                      margin-right="5px"
                                      padding-start="5px"
                                      padding-end="5px"
                                      padding-before="2px"
                                      padding-after="5px">
                                <fo:block role="H2"
                                          border-after-style="solid"
                                          border-color="#00A298"
                                          border-width="1.5px"
                                          xsl:use-attribute-sets="header-small">
                                    Now you have got your National Insurance number
                                </fo:block>
                                <fo:block role="Div"
                                          border-after-style="solid"
                                          border-width="1.5px"
                                          border-color="#00A298"
                                          padding-after="5px">
                                    <fo:block xsl:use-attribute-sets="small">
                                        You can download and use the HMRC App or go online at
                                        <fo:basic-link color="#531fff"
                                                       text-decoration="underline"
                                                       external-destination="www.gov.uk/personal-tax-account">
                                            www.gov.uk/personal-tax-account
                                        </fo:basic-link>
                                        where you can:
                                    </fo:block>
                                    <fo:list-block xsl:use-attribute-sets="list-block">
                                        <fo:list-item xsl:use-attribute-sets="small-list" role="LI">
                                            <fo:list-item-label role="Lbl" end-indent="label-end()">
                                                <fo:block>
                                                    <fo:inline>
                                                        <fo:wrapper role="artifact">
                                                            &#8226;
                                                        </fo:wrapper>
                                                    </fo:inline>
                                                </fo:block>
                                            </fo:list-item-label>
                                            <fo:list-item-body role="LBody" start-indent="body-start()">
                                                <fo:block>
                                                    create and access your Personal Tax Account
                                                </fo:block>
                                            </fo:list-item-body>
                                        </fo:list-item>
                                        <fo:list-item xsl:use-attribute-sets="small-list" role="LI">
                                            <fo:list-item-label role="Lbl" end-indent="label-end()">
                                                <fo:block>
                                                    <fo:inline>
                                                        <fo:wrapper role="artifact">
                                                            &#8226;
                                                        </fo:wrapper>
                                                    </fo:inline>
                                                </fo:block>
                                            </fo:list-item-label>
                                            <fo:list-item-body role="LBody" start-indent="body-start()">
                                                <fo:block>
                                                    save and print another copy of this letter
                                                </fo:block>
                                            </fo:list-item-body>
                                        </fo:list-item>
                                        <fo:list-item xsl:use-attribute-sets="small-list" role="LI">
                                            <fo:list-item-label role="Lbl" end-indent="label-end()">
                                                <fo:block>
                                                    <fo:inline>
                                                        <fo:wrapper role="artifact">
                                                            &#8226;
                                                        </fo:wrapper>
                                                    </fo:inline>
                                                </fo:block>
                                            </fo:list-item-label>
                                            <fo:list-item-body role="LBody" start-indent="body-start()">
                                                <fo:block>
                                                    tell us about a change to your address
                                                </fo:block>
                                            </fo:list-item-body>
                                        </fo:list-item>
                                        <fo:list-item xsl:use-attribute-sets="small-list" role="LI">
                                            <fo:list-item-label role="Lbl" end-indent="label-end()">
                                                <fo:block>
                                                    <fo:inline>
                                                        <fo:wrapper role="artifact">
                                                            &#8226;
                                                        </fo:wrapper>
                                                    </fo:inline>
                                                </fo:block>
                                            </fo:list-item-label>
                                            <fo:list-item-body role="LBody" start-indent="body-start()">
                                                <fo:block>
                                                    check your income tax estimate tax code
                                                </fo:block>
                                            </fo:list-item-body>
                                        </fo:list-item>
                                    </fo:list-block>
                                </fo:block>
                                <fo:block role="Div">
                                    <fo:block xsl:use-attribute-sets="small">
                                        View more information about National Insurance at:
                                    </fo:block>
                                    <fo:basic-link xsl:use-attribute-sets="small"
                                                   color="#531fff"
                                                   text-decoration="underline"
                                                   external-destination="www.gov.uk/national-insurance">
                                        www.gov.uk/national-insurance
                                    </fo:basic-link>
                                    <fo:block xsl:use-attribute-sets="small">
                                        or our YouTube channel at:
                                    </fo:block>
                                    <fo:basic-link xsl:use-attribute-sets="small"
                                                   color="#531fff"
                                                   text-decoration="underline"
                                                   external-destination="www.youtube.com/HMRCgovuk">
                                        www.youtube.com/HMRCgovuk
                                    </fo:basic-link>
                                </fo:block>
                            </fo:block>
                        </fo:inline-container>
                    </fo:block>


                    <!-- end text -->
                    <fo:block role="Div" xsl:use-attribute-sets="default-font">
                        <fo:block xsl:use-attribute-sets="large">
                            Information is available in large print, audio tape and Braille formats.
                        </fo:block>

                        <fo:block xsl:use-attribute-sets="large">
                            Text Relay service prefix number - 18001
                        </fo:block>
                    </fo:block>

                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
</xsl:stylesheet>