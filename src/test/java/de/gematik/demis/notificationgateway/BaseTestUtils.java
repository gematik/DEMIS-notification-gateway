package de.gematik.demis.notificationgateway;

/*-
 * #%L
 * DEMIS Notification-Gateway
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.notificationgateway.common.dto.LocationDTO;
import de.gematik.demis.notificationgateway.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.hl7.fhir.r4.model.Parameters;

public interface BaseTestUtils {
  String EXPIRED_IBM_TOKEN =
      "eyJhbGciOiJFUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJoVk4zenEzNVdoMHRxYVFIcC1fSmUtVzA5amNCc3pVYUF0a0NCSDZ"
          + "UU0Q4In0.eyJleHAiOjE2NDcwMTI1MzksImlhdCI6MTY0NzAxMjIzOSwiYXV0aF90aW1lIjoxNjQ3MDExMDA5LCJqdGkiOi"
          + "IzMjQ3OGJhNi0yOTVkLTQ0OWMtOGM5Yy00ZjE2MGIzYTNmMDIiLCJpc3MiOiJodHRwczovL2lkLmNlcnRpZnkuZGVtby51Ym"
          + "lyY2guY29tL2F1dGgvcmVhbG1zL2dlbWF0aWsiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiYjhlNjM3MjEtMTMyZS00ZTVhLT"
          + "g5NzktYjFmMjdlN2IxZWI2IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoic21jYi1hdXRoLWtvbWZvcnRjbGllbnQtc2ltdWxhdG"
          + "9yLWNhcmQyIiwibm9uY2UiOiI3MTcyNjkzNi1hMmNjLTQwMjAtYTBmYy0yMDc5ZGZhYzNhOTgiLCJzZXNzaW9uX3N0YXRlIj"
          + "oiNjEzZjJjYzgtYTQ2MC00MDE0LWJlMDMtYTVjM2JlMDNmOGRhIiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHR"
          + "wOi8vMTI3LjAuMC4xOjgwMDMiLCJodHRwOi8vMTI3LjAuMC4xOjgwMDIiLCJodHRwczovL2lkLmNlcnRpZnkuZGVtby51Ymly"
          + "Y2guY29tIiwiaHR0cDovLzEyNy4wLjAuMTo4MDAxIiwiaHR0cHM6Ly9pZC5ydS5pbXBmbmFjaHdlaXMuaW5mbyJdLCJyZWFsb"
          + "V9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1nZW1hdGlrIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXp"
          + "hdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2Ut"
          + "YWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJzaWQiOiI2M"
          + "TNmMmNjOC1hNDYwLTQwMTQtYmUwMy1hNWMzYmUwM2Y4ZGEiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6Ik1hcmllbmt"
          + "yYW5rZW5oYXVzVEVTVC1PTkxZIiwibG9jYXRpb24taWQiOiJUSTk2ODc2NDQ1QkU3ODhBMUQ0RUQ4MzkiLCJwcmVmZXJyZWRfdX"
          + "Nlcm5hbWUiOiI1LXNtYy1iLXRlc3RrYXJ0ZS04ODMxMTAwMDAxMTc5NDUiLCJwcm9mZXNzaW9uLW9pZCI6IjEuMi4yNzYuMC43Ni"
          + "40LjUzIiwiZmFtaWx5X25hbWUiOiJNYXJpZW5rcmFua2VuaGF1c1RFU1QtT05MWSJ9.ANXliWROuryCBnKdDTjTR_x1EqNWWiHPl"
          + "iLlF-fdHur2c1LdWgjooJayibIzGYIfFAkvsGq53Ka7mf0l4pc0xQ";

  String EXPIRED_DEMIS_HOSPITAL_TOKEN =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsS2VZeHdrbTBFdEN5eTJLUWhPZ0lvelZRYWlHX1BRZGZobFZOeW5yTTVFIn0.eyJleHAiOjE2OTgyMjUyMjQsImlhdCI6MTY5ODIyNDYyNCwianRpIjoiNjAwZDg1YzAtNDI4NS00ZDg5LWIyMzQtNGEwOTBhMWZiY2ViIiwiaXNzIjoiaHR0cHM6Ly9xcy5kZW1pcy5ya2kuZGUvYXV0aC9yZWFsbXMvSE9TUElUQUwiLCJhdWQiOlsibm90aWZpY2F0aW9uLWdhdGV3YXkiLCJub3RpZmljYXRpb24tZW50cnktc2VydmljZSJdLCJzdWIiOiJhNjBiMzkxNS01MjVlLTRiODItYjY5MS1iNjMwMTQzZGM2OTEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJkZW1pcy10ZXN0Iiwic2Vzc2lvbl9zdGF0ZSI6IjU2Y2YzM2M3LWNiYjktNDcyYi1iNTAxLWEwYTI0ZjZhMmNhZCIsInJlc291cmNlX2FjY2VzcyI6eyJub3RpZmljYXRpb24tZ2F0ZXdheSI6eyJyb2xlcyI6WyJsYWItbm90aWZpY2F0aW9uLXNlbmRlciIsImRpc2Vhc2Utbm90aWZpY2F0aW9uLXNlbmRlciJdfSwibm90aWZpY2F0aW9uLWVudHJ5LXNlcnZpY2UiOnsicm9sZXMiOlsibGFiLW5vdGlmaWNhdGlvbi1zZW5kZXIiLCJkaXNlYXNlLW5vdGlmaWNhdGlvbi1zZW5kZXIiXX19LCJzY29wZSI6InByb2ZpbGUiLCJzaWQiOiI1NmNmMzNjNy1jYmI5LTQ3MmItYjUwMS1hMGEyNGY2YTJjYWQiLCJpayI6IjA5ODc2NTQzMiIsIm9yZ2FuaXphdGlvbiI6ImdlbWF0aWsgR21iSCIsInByZWZlcnJlZF91c2VybmFtZSI6IjExMTExIn0.MbmzQjgt5agwWBrNxz_GnnWFSnNndqW6vwxMfRrbvkAaE2w731jBpiFgYyV0jljsl7-qQHvxxzHBE7Mkw22hvp6zLYYrZD5U-QOGiPV04r1mDEXsTQO4D039kCvvDVP5Mn98K-N4CgdstRp0tZNaLfWfaZ2jwtaqSejWMJ4qHQs43AaPs6BGDYAm2Ct6IqFvM_RfiyDeQemH58IgpObuK1lWkdthvAJP-SlGKSbU8xDNh_Rg2A7vOGlPMr5PBgEQfVpQWMMJQ7kbk7KuKL-B6KozGbXn0INnv6Z3RKiA7TwMBFEbXeQITQF3W6Rc_W88yAighvk2G-LD6jQwUTRUgQ";

  String EXPIRED_DEMIS_LAB_TOKEN =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJsS2VZeHdrbTBFdEN5eTJLUWhPZ0lvelZRYWlHX1BRZGZobFZOeW5yTTVFIn0.eyJleHAiOjE2OTgyMjUyODksImlhdCI6MTY5ODIyNDY4OSwianRpIjoiYzZiOWM5OGQtM2E2Mi00MTZmLWJjZjgtNDhhOTcxNDMyMWY1IiwiaXNzIjoiaHR0cHM6Ly9xcy5kZW1pcy5ya2kuZGUvYXV0aC9yZWFsbXMvTEFCIiwiYXVkIjoibm90aWZpY2F0aW9uLWFwaSIsInN1YiI6ImJkZjIxNWM2LWEyYmMtNDhjMS04ODBiLTQ0MTVkNWI5M2EzNCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRlbWlzLWFkYXB0ZXIiLCJzZXNzaW9uX3N0YXRlIjoiMjM1ZjBhYTgtNmY2ZS00MmUzLTgxOTAtMTQ3MTA5MWYxM2I0IiwicmVzb3VyY2VfYWNjZXNzIjp7Im5vdGlmaWNhdGlvbi1hcGkiOnsicm9sZXMiOlsibGFiLW5vdGlmaWNhdGlvbi1zZW5kZXIiXX19LCJzY29wZSI6InByb2ZpbGUiLCJzaWQiOiIyMzVmMGFhOC02ZjZlLTQyZTMtODE5MC0xNDcxMDkxZjEzYjQiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiIxMTExMSJ9.Tvf4zaa2Qtvj6FviLF-6MtfEr8d7S2XBanGT6-8HkpGWMev3ZR8BFx68IoPLX0ZBPhibjW5kycLA4CBsJKsHIjuGR86ca2K4Xi3s6vOPhSHqwzMAoKbhyg_74I2pUpX2BUYMxWtTuFaIw0smv0oUXP6fo6ob_Mq24i3n8RQQUlXXQenQvE53Eg8RKPG3lRe8gWqvjG5Stz2It5BTwCciPQnWvSflADQyBsFbJZlin0s79XsB2ZgAM1oxoj0xaLwWvaGUBiOm7Btwz2HRPeKRSvfP0qice4YpdtaifSn4XUqjTgFvSxqcbA0L_AUjFpp44QUkiIDmCEeKKtWhr8uMoQ";

  String EXPIRED_DEMIS_PORTAL_TOKEN_HOSPITAL =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJyUldwMUlGX0hNQ0EwZFFpVHA2X2VMVDNKSXdmNVVnalA3M05HZnFEYllNIn0.eyJleHAiOjE3MTI4MjY3MTksImlhdCI6MTcxMjgyNjQxOSwiYXV0aF90aW1lIjoxNzEyODI2NDE5LCJqdGkiOiJmZjU0NDk2Ni1mNDIxLTQ3ZWMtODU1ZC01ZjdhNzY5MmU0ZTYiLCJpc3MiOiJodHRwczovL2F1dGguaW5ncmVzcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjU0ODg5MjkyLWY2MGMtNDAzNy04NDU2LTY1M2Y4NDhjMWUwYSIsInR5cCI6IkJlYXJlciIsImF6cCI6Im1lbGRlcG9ydGFsIiwibm9uY2UiOiI2ODliMmVlOTk4MTUyYmRjNDViNDI2ZTFlNjEzNWVjZGMzVjJ3VVVmUyIsInNlc3Npb25fc3RhdGUiOiIxODFjOGYyMS0xYjAzLTRlNmMtOWM0My1iZmJhMDIxOWQyZTkiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vcG9ydGFsLmluZ3Jlc3MubG9jYWwiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImJlZC1vY2N1cGFuY3ktc2VuZGVyIiwiZGlzZWFzZS1ub3RpZmljYXRpb24tc2VuZGVyIiwib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLXBvcnRhbCIsInVtYV9hdXRob3JpemF0aW9uIiwicGF0aG9nZW4tbm90aWZpY2F0aW9uLXNlbmRlciIsInZhY2NpbmUtaW5qdXJ5LXNlbmRlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJzaWQiOiIxODFjOGYyMS0xYjAzLTRlNmMtOWM0My1iZmJhMDIxOWQyZTkiLCJpayI6IjEyMzQ5NDU0NiIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJvZmVzc2lvbk9pZCI6IjEuMi4yNzYuMC43Ni40LjUzIiwib3JnYW5pemF0aW9uTmFtZSI6IktyYW5rZW5oYXVzIE1lbGlzc2EgRGF2aWQgVEVTVC1PTkxZIiwiYWNjb3VudFR5cGUiOiJvcmdhbml6YXRpb24iLCJhY2NvdW50U291cmNlIjoiZ2VtYXRpayIsImFjY291bnRJc1RlbXBvcmFyeSI6dHJ1ZSwiYWNjb3VudElkZW50aWZpZXIiOiJodHRwczovL2dlbWF0aWsuZGUvZmhpci9zaWQvdGVsZW1hdGlrLWlkfDUtMi0xMjM0OTQ1NDYiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiI1LTItMTIzNDk0NTQ2IiwidXNlcm5hbWUiOiI1LTItMTIzNDk0NTQ2In0.J5MRrZkgiX1E8ehOTnA9rmc_bAYoRZA0c3iTYKX3TkIy7AJjGliAWsj_5mEG4zA20eG8Q4f05wvNpUp1QiOcNjN_ludObEus3vCpD63vqm_YG1zVNUdIpqH0owwJYYdnRtFC3LJWGEDKESoN0B6YvhzemufOdgvaEuOvqUYnJhoxsZQHS3NsdDMOPPosBTinEgbMmTm6CjbOt3GS28ezSFt2sm2KV_uqRFXSgKv92FtrRxkYJpipx2y02OCWQ8bjlg2kFkgCrVgt9ws1V2eNz4g_RW5S7Sv9lFe9RE0YHlZFqztEmojHi-BwiipzWoExpaO9XkIKibv6Pc9yyi4MBg";

  String EXPIRED_DEMIS_PORTAL_TOKEN_PRAXIS =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJyUldwMUlGX0hNQ0EwZFFpVHA2X2VMVDNKSXdmNVVnalA3M05HZnFEYllNIn0.eyJleHAiOjE3MTI4NTMxMzcsImlhdCI6MTcxMjg1MjgzNywiYXV0aF90aW1lIjoxNzEyODUyODM3LCJqdGkiOiJiOGRkY2MwOC1lMmZmLTRmMjItODg4NC05MzJjMGYyYjE2OTciLCJpc3MiOiJodHRwczovL2F1dGguaW5ncmVzcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjQ4YjNkNDA5LWYyNzgtNDM1ZS05NDhjLTRhNDJiZDNmN2E0MyIsInR5cCI6IkJlYXJlciIsImF6cCI6Im1lbGRlcG9ydGFsIiwibm9uY2UiOiIyNDFjNWExYzJiYmU1OTUzMDQ0YjVlNzVjMGM4NDQ4ZTVhZ3RXTkUwSCIsInNlc3Npb25fc3RhdGUiOiJmNjkyMmQyNi1hYWZlLTRkMWEtYTNmNi1kNWM0ZGYxNjljOGQiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbImh0dHBzOi8vcG9ydGFsLmluZ3Jlc3MubG9jYWwiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRpc2Vhc2Utbm90aWZpY2F0aW9uLXNlbmRlciIsIm9mZmxpbmVfYWNjZXNzIiwiZGVmYXVsdC1yb2xlcy1wb3J0YWwiLCJ1bWFfYXV0aG9yaXphdGlvbiIsInBhdGhvZ2VuLW5vdGlmaWNhdGlvbi1zZW5kZXIiLCJ2YWNjaW5lLWluanVyeS1zZW5kZXIiXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwic2lkIjoiZjY5MjJkMjYtYWFmZS00ZDFhLWEzZjYtZDVjNGRmMTY5YzhkIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcm9mZXNzaW9uT2lkIjoiMS4yLjI3Ni4wLjc2LjQuNTAiLCJvcmdhbml6YXRpb25OYW1lIjoiQXJ6dHByYXhpcyBBbm4tQmVhdHJpeGUgWXVjY2FwYWxtZSBURVNULU9OTFkiLCJhY2NvdW50VHlwZSI6Im9yZ2FuaXphdGlvbiIsImFjY291bnRTb3VyY2UiOiJnZW1hdGlrIiwiYWNjb3VudElzVGVtcG9yYXJ5Ijp0cnVlLCJhY2NvdW50SWRlbnRpZmllciI6Imh0dHBzOi8vZ2VtYXRpay5kZS9maGlyL3NpZC90ZWxlbWF0aWstaWR8MS0yMDAxNDI0MjQyNCIsInByZWZlcnJlZF91c2VybmFtZSI6IjEtMjAwMTQyNDI0MjQiLCJ1c2VybmFtZSI6IjEtMjAwMTQyNDI0MjQifQ.lCZcQfXI6Ylach8fWa6FN1QHTmoavnkyzYc_I4ZwDjhG9GzynDXmefp84NMslgk5_2VdxldaS6lfocGlS8pdXou-MINgWuvHqZNF11CvoL1SeMvIwgRKrv7nSYNqPz3USQXCwXt0t1YFPCuW0lVSCLG6hcWdmlFZNCu1QSUUB3gBigNCBzhRbpJTUhgKXKRCleU6r4Ed7mSQVjVXmzeV-utV38vCfR5AdOKr8JN3xv9NoKRIVehijTbwvXRdQhBr6l0435QGvxRmud904BhI6i5QX4ytJphz0QNvGLV1w8pzJLREYq2T1DJ3BzLNRtQi1gPqtB4gj-bQdCB2PVnTjw";

  String TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
          + ".eyJpc3MiOiJodHRwczovL2F1dGguaW5ncmV"
          + "zcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiaWF"
          + "0IjoxNzAxNzgzNDY5LCJleHAiOjE3MDE3ODM"
          + "0NzEsImF1ZCI6Im1lbGRlcG9ydGFsIiwic3V"
          + "iIjoiNmRlZDlkZWItYTYyZi00MWI2LTlkZDY"
          + "tMmU2MzE0YjY2ZWVjIn0.6-q0bOuBPjfTa35"
          + "ZmxRPJ7Dhe_guzz5zXl2LlJPWbOo";

  String TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_2_260550131 =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
          + ".eyJpc3MiOiJodHRwczovL2F1dGguaW5ncmV"
          + "zcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiaWF"
          + "0IjoxNzAxNzgzNDY5LCJleHAiOjE3MDE3ODM"
          + "0NzEsImF1ZCI6Im1lbGRlcG9ydGFsIiwic3V"
          + "iIjoiNmRlZDlkZWItYTYyZi00MWI2LTlkZDY"
          + "tMmU2MzE0YjY2ZWVjIiwicHJlZmVycmVkX3V"
          + "zZXJuYW1lIjoiNS0yLTI2MDU1MDEzMSJ9.ph"
          + "K6RrZqa99tom1NCxUJ0WNV9CWa4VvNl4P9ey"
          + "6kLhg";

  String TOKEN_WITH_NO_IK_AND_PREFERRED_USERNAME_5_3_123456789 =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
          + ".eyJpc3MiOiJodHRwczovL2F1dGguaW5ncmV"
          + "zcy5sb2NhbC9yZWFsbXMvUE9SVEFMIiwiaWF"
          + "0IjoxNzAxNzgzNDY5LCJleHAiOjE3MDE3ODM"
          + "0NzEsImF1ZCI6Im1lbGRlcG9ydGFsIiwic3V"
          + "iIjoiNmRlZDlkZWItYTYyZi00MWI2LTlkZDY"
          + "tMmU2MzE0YjY2ZWVjIiwicHJlZmVycmVkX3Vz"
          + "ZXJuYW1lIjoiNS0zLTEyMzQ1Njc4OSJ9.EU8H"
          + "6WdnRvq_eJV1OIJWVnaXnHXrlUwG1HnFDQ-1f"
          + "aI";

  default String getLaboratoryString() {
    return FileUtils.loadJsonFromFile("/portal/laboratory/notification_content_min.json");
  }

  default String getJsonContent(String location) throws URISyntaxException, IOException {
    Objects.requireNonNull(location, "require nonNull location");
    final URL resource = getClass().getClassLoader().getResource(location);
    Objects.requireNonNull(resource, "location is invalid");
    File file = new File(resource.toURI());
    return Files.readString(file.toPath());
  }

  default LocationDTO createLocation() {
    return new LocationDTO()
        .id(ThreadLocalRandom.current().nextInt())
        .ik(RandomStringUtils.randomNumeric(9))
        .label(RandomStringUtils.randomAlphabetic(5))
        .postalCode(RandomStringUtils.randomNumeric(5))
        .city(RandomStringUtils.randomAlphabetic(5))
        .line(RandomStringUtils.randomAlphabetic(5))
        .houseNumber(RandomStringUtils.randomAlphabetic(3));
  }

  default String pdfToText(byte[] pdfContent) throws IOException {
    PDDocument doc = Loader.loadPDF(pdfContent);
    PDFTextStripper findPhrase = new PDFTextStripper();
    return findPhrase.getText(doc);
  }

  default Parameters createJsonOkParameters(String path) throws URISyntaxException, IOException {
    return (Parameters) FhirContext.forR4().newJsonParser().parseResource(getJsonContent(path));
  }
}
