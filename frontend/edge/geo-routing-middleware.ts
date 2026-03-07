/**
 * Edge geo-routing middleware example.
 * Deploy this in your edge runtime (Cloudflare/Vercel/Netlify) to route users by country.
 */
export default async function geoRoutingMiddleware(request: Request): Promise<Response> {
  const url = new URL(request.url)
  const country =
    request.headers.get('x-vercel-ip-country') ??
    request.headers.get('cf-ipcountry') ??
    request.headers.get('x-country-code') ??
    'US'

  const currentPrefix = url.pathname.split('/')[1]
  const targetPrefix =
    country === 'US' || country === 'CA'
      ? 'us'
      : ['DE', 'FR', 'ES', 'IT', 'NL', 'SE', 'GB', 'IE'].includes(country)
        ? 'eu'
        : 'apac'

  const hasRegionPrefix = ['us', 'eu', 'apac'].includes(currentPrefix)
  if (!hasRegionPrefix) {
    url.pathname = `/${targetPrefix}${url.pathname}`
    return Response.redirect(url, 307)
  }

  return fetch(request)
}
