package dev.orhantugrul.craftgate.common.util;

import dev.orhantugrul.craftgate.common.data.Credentials;
import dev.orhantugrul.craftgate.common.data.Options;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

public final class Request {
  private Request() {
    throw new IllegalStateException();
  }

  /**
   * Constructs a URL by combining the base URL from the provided {@link Options} object with the
   * specified path and an optional parameterizable query string.
   *
   * @param path    the specific endpoint path to be appended to the base URL
   * @param options the configuration options containing the base URL
   * @return the full URL as a string
   */
  public static String url(final String path, final Options options) {
    return url(path, null, options);
  }

  /**
   * Constructs a URL by combining the base URL from {@link Options}, the specified path, and an
   * optional query string generated from the {@link Parameterizable} object.
   *
   * @param path            the specific path to be appended to the base URL
   * @param parameterizable an optional object whose parameters will be converted into an encoded
   * @param options         the configuration options containing the base URL and other settings
   *                        query string and appended to the URL
   * @return the complete URL constructed with the base URL, path, and optional query string
   */
  public static String url(
      final String path,
      final Parameterizable parameterizable,
      final Options options) {
    final var query = Optional.ofNullable(parameterizable).map(Request::query).orElse("");
    return options.base() + path + query;
  }

  /**
   * Constructs a query string with URL-encoded parameters from the provided {@link Parameterizable}
   * object.
   *
   * @param parameterizable the object whose parameters will be converted into a query string
   * @return a query string in the format "?key1=value1&key2=value2" with URL-encoded values
   */
  public static String query(final Parameterizable parameterizable) {
    final var joiner = new StringJoiner("&", "?", "");

    for (final var entry : parameterizable.toParameters().entrySet()) {
      final var key = entry.getKey();
      final var value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      joiner.add(key + "=" + value);
    }

    return joiner.toString();
  }

  /**
   * Generates a map of HTTP headers required for making API requests. This method includes
   * authentication and meta-information headers.
   *
   * @param url         the API endpoint URL for which the headers are being generated
   * @param credentials the credentials containing API key and secret key
   * @param options     the options containing configurations such as base URL and language
   * @return a map containing HTTP header names and their corresponding values
   */
  public static Map<String, String> headers(
      final String url,
      final Credentials credentials,
      final Options options) {
    return headers(url, null, credentials, options);
  }

  /**
   * Generates HTTP headers for a request based on the given URL, payload, credentials, and options.
   * Creates a signature for authentication and includes additional required metadata.
   *
   * @param url         the request URL to be signed and included in the headers
   * @param payload     the request payload; may include data to be sent with the request, or null
   * @param credentials the credentials used for authentication and signature generation
   * @param options     the configuration options containing additional parameters such as language
   * @return a map of HTTP headers containing authentication and metadata information
   */
  public static Map<String, String> headers(
      final String url,
      final Object payload,
      final Credentials credentials,
      final Options options) {
    final var secret = credentials.apiKey() + credentials.secretKey();
    final var decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
    final var json = Optional.ofNullable(payload).map(Json::to).orElse("");
    final var random = UUID.randomUUID().toString();

    final var encode = Cryptology.base64(secret + decodedUrl + json + random);
    final var signature = Cryptology.sha256(encode);

    return Map.ofEntries(
        Map.entry("x-api-key", credentials.apiKey()),
        Map.entry("x-rnd-key", random),
        Map.entry("x-auth-version", "v1"),
        Map.entry("x-client-version", "craftgate-java-client:1.0.69"),
        Map.entry("x-signature", signature),
        Map.entry("lang", options.language()));
  }
}
