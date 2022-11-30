/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import java.io.StringReader
import javax.xml.transform.stream.StreamSource
object ApacheFOPHelpers {
  def xmlData(title: String, firstName: String, lastName: String, nino: String, add1: String, add2: String, add3: String) = {
    val xmlInput =
      s"""
    <root>
         |    <title>${title}</title>
         |    <first-name>${firstName.toUpperCase}</first-name>
         |    <last-name>${lastName.toUpperCase}</last-name>
         |    <address-line-1>${add1}</address-line-1>
         |    <address-line-2>${add2}</address-line-2>
         |    <address-line-3>${add3}</address-line-3>
         |    <nino>${nino}</nino>
         |</root>
         |
         |""".stripMargin
    new StreamSource(new StringReader(xmlInput))
  }

  def xslData = {
    val xsld =
      s"""<?xml version="1.0" encoding="utf-8"?>
         |<xsl:stylesheet version="1.0"
         |                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
         |                xmlns:fo="http://www.w3.org/1999/XSL/Format"
         |                xmlns:fox="http://xmlgraphics.apache.org/fop/extensions">
         |
         |    <xsl:attribute-set name="normal">
         |        <xsl:attribute name="font-size">10.5pt</xsl:attribute>
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |        <xsl:attribute name="padding-before">6px</xsl:attribute>
         |        <xsl:attribute name="padding-after">6px</xsl:attribute>
         |        <xsl:attribute name="role">P</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="small">
         |        <xsl:attribute name="font-size">10pt</xsl:attribute>
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |        <xsl:attribute name="padding-before">6px</xsl:attribute>
         |        <xsl:attribute name="padding-after">6px</xsl:attribute>
         |
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="normal-list">
         |        <xsl:attribute name="font-size">10.5pt</xsl:attribute>
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |        <xsl:attribute name="line-height">12pt</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="list-block">
         |        <xsl:attribute name="provisional-distance-between-starts">0.3cm</xsl:attribute>
         |        <xsl:attribute name="role">L</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="large">
         |        <xsl:attribute name="font-size">16pt</xsl:attribute>
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |        <xsl:attribute name="padding-before">6px</xsl:attribute>
         |        <xsl:attribute name="padding-after">6px</xsl:attribute>
         |        <xsl:attribute name="role">P</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="header-small">
         |        <xsl:attribute name="font-size">10.5pt</xsl:attribute>
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |        <xsl:attribute name="font-weight">bold</xsl:attribute>
         |        <xsl:attribute name="padding-before">6px</xsl:attribute>
         |        <xsl:attribute name="padding-after">6px</xsl:attribute>
         |        <xsl:attribute name="role">H2</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="default-font-and-padding">
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |        <xsl:attribute name="padding-before">6px</xsl:attribute>
         |        <xsl:attribute name="padding-after">6px</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:attribute-set name="default-font">
         |        <xsl:attribute name="font-family">Helvetica</xsl:attribute>
         |    </xsl:attribute-set>
         |
         |    <xsl:output method="xml" indent="yes"/>
         |    <xsl:template match="/">
         |        <fo:root xml:lang="en">
         |            <fo:layout-master-set>
         |                <!-- layout for the first page -->
         |                <fo:simple-page-master master-name="first"
         |                                       page-height="29.7cm"
         |                                       page-width="21cm"
         |                                       margin-top="1cm"
         |                                       margin-bottom="1.5cm"
         |                                       margin-left="3cm"
         |                                       margin-right="3cm">
         |                    <fo:region-body margin-top="1.5cm"/>
         |                    <fo:region-before extent="0.5cm"/>
         |                    <fo:region-after extent="1.5cm"/>
         |                </fo:simple-page-master>
         |
         |                <!-- layout for the other pages -->
         |                <fo:simple-page-master master-name="rest"
         |                                       page-height="29.7cm"
         |                                       page-width="21cm"
         |                                       margin-top="1cm"
         |                                       margin-bottom="1.5cm"
         |                                       margin-left="3cm"
         |                                       margin-right="3cm">
         |                    <fo:region-body margin-top="1cm"/>
         |                    <fo:region-before extent="0.5cm"/>
         |                    <fo:region-after extent="1.5cm"/>
         |                </fo:simple-page-master>
         |
         |                <fo:page-sequence-master master-name="basicPSM">
         |                    <fo:repeatable-page-master-alternatives>
         |                        <fo:conditional-page-master-reference master-reference="first"
         |                                                              page-position="first"/>
         |                        <fo:conditional-page-master-reference master-reference="rest"
         |                                                              page-position="rest"/>
         |                        <!-- recommended fallback procedure -->
         |                        <fo:conditional-page-master-reference master-reference="rest"/>
         |                    </fo:repeatable-page-master-alternatives>
         |                </fo:page-sequence-master>
         |
         |            </fo:layout-master-set>
         |            <!-- end: defines page layout -->
         |
         |            <!-- actual layout -->
         |            <fo:page-sequence master-reference="basicPSM">
         |                <!-- body -->
         |                <fo:flow flow-name="xsl-region-body">
         |
         |                    <!-- logo and heading -->
         |                    <fo:block space-after="10px">
         |                        <fo:inline-container inline-progression-dimension="22%">
         |                            <fo:block border-left-style="solid"
         |                                      border-width="2px"
         |                                      border-color="#28a197"
         |                                      padding-start="4px">
         |                                <fo:external-graphic src="images/HMRC-logo.jpg" fox:alt-text="HMRC" content-height="scale-to-fit"  content-width="0.8cm"/>
         |                                <fo:block xsl:use-attribute-sets="default-font"
         |                                          line-height="14pt"
         |                                          font-size="14pt">
         |                                    HM Revenue &amp; Customs
         |                                </fo:block>
         |                            </fo:block>
         |                        </fo:inline-container>
         |                        <fo:inline-container inline-progression-dimension="78%">
         |                            <fo:block role="H1"
         |                                      font-weight="bold"
         |                                      text-align="end"
         |                                      xsl:use-attribute-sets="default-font-and-padding">
         |                                Your National Insurance letter
         |                            </fo:block>
         |                        </fo:inline-container>
         |                    </fo:block>
         |
         |
         |                    <!-- Addresses -->
         |                    <fo:block>
         |                        <fo:inline-container inline-progression-dimension="63%">
         |                            <fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                    <xsl:value-of select="root/title"/>
         |                                    <xsl:value-of select="concat(' ', substring(root/first-name, 1, 1))"/>
         |                                    <xsl:value-of select="concat(' ', root/last-name)"/>
         |                                </fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                    <xsl:value-of select="root/address-line-1"/>
         |                                </fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                    <xsl:value-of select="root/address-line-2"/>
         |                                </fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                    <xsl:value-of select="root/address-line-3"/>
         |                                </fo:block>
         |                            </fo:block>
         |                        </fo:inline-container>
         |                        <fo:inline-container inline-progression-dimension="37%">
         |                            <fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">NIC&amp;EO</fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">HMRC</fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal-list">BX9 1AN</fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal">Phone&#x9;0300 200 3500</fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal">www.gov.uk/hmrc</fo:block>
         |                            </fo:block>
         |                        </fo:inline-container>
         |                    </fo:block>-
         |
         |
         |                    <!-- name and title -->
         |                    <fo:block xsl:use-attribute-sets="normal">
         |                        <xsl:value-of select="root/title"/>
         |                        <xsl:value-of select="concat(' ', root/first-name)"/>
         |                        <xsl:value-of select="concat(' ', root/last-name)"/>
         |                    </fo:block>
         |
         |
         |                    <!-- NINO number box -->
         |                    <fo:block space-after="10px"
         |                            background-color="#DAF4F2"
         |                            border-style="solid"
         |                            border-width="1.5px"
         |                            border-color="#28a197">
         |                        <fo:block xsl:use-attribute-sets="header-small"
         |                                  text-align="center">
         |                            Your National Insurance number is
         |                        </fo:block>
         |                        <fo:block font-size="20pt"
         |                                  font-weight="bold"
         |                                  text-align="center"
         |                                  xsl:use-attribute-sets="default-font">
         |                            <xsl:value-of select="root/nino"/>
         |                        </fo:block>
         |                    </fo:block>
         |
         |                    <fo:block space-after="10px"
         |                              font-weight="bold"
         |                              text-align="center"
         |                              xsl:use-attribute-sets="default-font-and-padding">
         |                        Keep this number in a safe place. Do not destroy this letter.
         |                    </fo:block>
         |
         |
         |                    <!-- about NINO information -->
         |                    <fo:block>
         |                        <fo:inline-container inline-progression-dimension="63%">
         |                            <fo:block margin-left="5px"
         |                                      margin-right="5px">
         |                                <fo:block role="H2"
         |                                          xsl:use-attribute-sets="header-small">
         |                                    About your National Insurance number
         |                                </fo:block>
         |
         |                                <fo:block xsl:use-attribute-sets="normal">
         |                                    Your National Insurance number is unique to you and will never change. To
         |                                    prevent identity fraud, do not share it with anyone who does not need it.
         |                                </fo:block>
         |
         |                                <fo:block xsl:use-attribute-sets="normal">
         |                                    You will need it if you:
         |                                </fo:block>
         |                                <fo:list-block xsl:use-attribute-sets="list-block">
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                start work (including part time and weekend jobs)
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                apply for a driving license
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                apply for a student loan
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                claim state benefits
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                register to vote
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                </fo:list-block>
         |                                <fo:block xsl:use-attribute-sets="normal">
         |                                    It is not proof of:
         |                                </fo:block>
         |                                <fo:list-block xsl:use-attribute-sets="list-block">
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                your identity
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                your right to work in the UK
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                </fo:list-block>
         |                                <fo:block role="H2"
         |                                          xsl:use-attribute-sets="header-small">
         |                                    Child Trust Fund
         |                                </fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal">
         |                                    When you turn 16, take control of your Child Trust Fund. Ask your parents or
         |                                    guardian, for more information go to
         |                                    <fo:basic-link color="#531fff"
         |                                                   text-decoration="underline"
         |                                                   external-destination="www.gov.uk/child-trust-funds">
         |                                        www.gov.uk/child-trust-funds
         |                                    </fo:basic-link>
         |                                </fo:block>
         |
         |                                <fo:block role="H2"
         |                                          xsl:use-attribute-sets="header-small">
         |                                    Welsh language
         |                                </fo:block>
         |                                <fo:block xsl:use-attribute-sets="normal">
         |                                    To continue to receive a Welsh language service:
         |                                </fo:block>
         |                                <fo:list-block xsl:use-attribute-sets="list-block">
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                email gwasanaeth.cymraeg@hmrc.gov.uk
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                    <fo:list-item>
         |                                        <fo:list-item-label end-indent="label-end()">
         |                                            <fo:block>
         |                                                <fo:inline>&#8226;</fo:inline>
         |                                            </fo:block>
         |                                        </fo:list-item-label>
         |                                        <fo:list-item-body start-indent="body-start()">
         |                                            <fo:block xsl:use-attribute-sets="normal-list">
         |                                                phone 0300 200 1900
         |                                            </fo:block>
         |                                        </fo:list-item-body>
         |                                    </fo:list-item>
         |                                </fo:list-block>
         |                            </fo:block>
         |                        </fo:inline-container>
         |                        <fo:inline-container inline-progression-dimension="37%">
         |                            <fo:block background-color="#DAF4F2"
         |                                      margin-left="5px"
         |                                      margin-right="5px"
         |                                      padding-start="5px"
         |                                      padding-end="5px"
         |                                      padding-before="2px"
         |                                      padding-after="5px">
         |                                <fo:block role="H2"
         |                                          border-after-style="solid"
         |                                          border-color="#00A298"
         |                                          border-width="1.5px"
         |                                          xsl:use-attribute-sets="header-small">
         |                                    Now you have got your National Insurance number
         |                                </fo:block>
         |                                <fo:block
         |                                        border-after-style="solid"
         |                                        border-width="1.5px"
         |                                        border-color="#00A298"
         |                                        padding-after="5px">
         |                                    <fo:block xsl:use-attribute-sets="small">
         |                                        You can download and use the HMRC App or go online at
         |                                        <fo:basic-link color="#531fff"
         |                                                       text-decoration="underline"
         |                                                       external-destination="www.gov.uk/personal-tax-account">
         |                                            www.gov.uk/personal-tax-account
         |                                        </fo:basic-link>
         |                                        where you can:
         |                                    </fo:block>
         |                                    <fo:list-block xsl:use-attribute-sets="list-block">
         |                                        <fo:list-item>
         |                                            <fo:list-item-label end-indent="label-end()">
         |                                                <fo:block>
         |                                                    <fo:inline>&#8226;</fo:inline>
         |                                                </fo:block>
         |                                            </fo:list-item-label>
         |                                            <fo:list-item-body start-indent="body-start()">
         |                                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                                    create and access your Personal Tax Account
         |                                                </fo:block>
         |                                            </fo:list-item-body>
         |                                        </fo:list-item>
         |                                        <fo:list-item>
         |                                            <fo:list-item-label end-indent="label-end()">
         |                                                <fo:block>
         |                                                    <fo:inline>&#8226;</fo:inline>
         |                                                </fo:block>
         |                                            </fo:list-item-label>
         |                                            <fo:list-item-body start-indent="body-start()">
         |                                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                                    save and print another copy of this letter
         |                                                </fo:block>
         |                                            </fo:list-item-body>
         |                                        </fo:list-item>
         |                                        <fo:list-item>
         |                                            <fo:list-item-label end-indent="label-end()">
         |                                                <fo:block>
         |                                                    <fo:inline>&#8226;</fo:inline>
         |                                                </fo:block>
         |                                            </fo:list-item-label>
         |                                            <fo:list-item-body start-indent="body-start()">
         |                                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                                    tell us about a change to your address
         |                                                </fo:block>
         |                                            </fo:list-item-body>
         |                                        </fo:list-item>
         |                                        <fo:list-item>
         |                                            <fo:list-item-label end-indent="label-end()">
         |                                                <fo:block>
         |                                                    <fo:inline>&#8226;</fo:inline>
         |                                                </fo:block>
         |                                            </fo:list-item-label>
         |                                            <fo:list-item-body start-indent="body-start()">
         |                                                <fo:block xsl:use-attribute-sets="normal-list">
         |                                                    check your income tax estimate tax code
         |                                                </fo:block>
         |                                            </fo:list-item-body>
         |                                        </fo:list-item>
         |                                    </fo:list-block>
         |                                </fo:block>
         |                                <fo:block>
         |                                    <fo:block xsl:use-attribute-sets="small">
         |                                        View more information about National Insurance at:
         |                                    </fo:block>
         |                                    <fo:basic-link xsl:use-attribute-sets="small"
         |                                                   color="#531fff"
         |                                                   text-decoration="underline"
         |                                                   external-destination="www.gov.uk/national-insurance">
         |                                        www.gov.uk/national-insurance
         |                                    </fo:basic-link>
         |                                    <fo:block xsl:use-attribute-sets="small">
         |                                        or our YouTube channel at:
         |                                    </fo:block>
         |                                    <fo:basic-link xsl:use-attribute-sets="small"
         |                                                   color="#531fff"
         |                                                   text-decoration="underline"
         |                                                   external-destination="www.youtube.com/HMRCgovuk">
         |                                        www.youtube.com/HMRCgovuk
         |                                    </fo:basic-link>
         |                                </fo:block>
         |                            </fo:block>
         |                        </fo:inline-container>
         |                    </fo:block>
         |
         |
         |                    <!-- end text -->
         |                    <fo:block>
         |                        <fo:block xsl:use-attribute-sets="large">
         |                            Information is available in large print, audio tape and Braille formats.
         |                        </fo:block>
         |
         |                        <fo:block xsl:use-attribute-sets="large">
         |                            Text Relay service prefix number - 18001
         |                        </fo:block>
         |                    </fo:block>
         |
         |                </fo:flow>
         |            </fo:page-sequence>
         |        </fo:root>
         |    </xsl:template>
         |</xsl:stylesheet>""".stripMargin
    new StreamSource(new StringReader(xsld))
  }


}
