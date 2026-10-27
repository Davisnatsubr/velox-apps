import UIKit
import WebKit

/// O site (Velox ou Pulso) dentro do app. Os vídeos em tela cheia abrem no PlayerViewController (VLC).
/// O site já conversa com o app Android por window.AndroidApp; aqui criamos o mesmo objeto, então nada muda no site.
final class WebViewController: UIViewController, WKScriptMessageHandler, WKNavigationDelegate, WKUIDelegate {

    private var web: WKWebView!
    private var errorView: UIView?
    private let info = Bundle.main.infoDictionary ?? [:]
    private lazy var siteURL = URL(string: info["SiteURL"] as? String ?? "about:blank")!
    private lazy var brand = UIColor(hex: info["BrandColor"] as? String ?? "#000000")

    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = brand

        let ver = info["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Int(info["CFBundleVersion"] as? String ?? "1") ?? 1
        let bridge = """
        window.AndroidApp = {
          play: function (j) { window.webkit.messageHandlers.app.postMessage({ cmd: 'play', data: j }); },
          version: function () { return '\(ver)'; },
          versionCode: function () { return \(build); },
          canUpdate: function () { return false; },
          isTv: function () { return false; },
          update: function () {},
          exit: function () {}
        };
        """
        let cfg = WKWebViewConfiguration()
        cfg.allowsInlineMediaPlayback = true
        cfg.mediaTypesRequiringUserActionForPlayback = []
        cfg.applicationNameForUserAgent = "Mobile/15E148 VeloxApp/\(ver) iOS"
        cfg.userContentController.addUserScript(WKUserScript(source: bridge, injectionTime: .atDocumentStart, forMainFrameOnly: true))
        cfg.userContentController.add(self, name: "app")

        web = WKWebView(frame: view.bounds, configuration: cfg)
        web.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        web.isOpaque = false
        web.backgroundColor = brand
        web.scrollView.backgroundColor = brand
        web.scrollView.bounces = false
        web.navigationDelegate = self
        web.uiDelegate = self
        view.addSubview(web)
        web.load(URLRequest(url: siteURL))
    }

    // MARK: site -> app
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any], (body["cmd"] as? String) == "play",
              let s = body["data"] as? String, let d = s.data(using: .utf8),
              let o = (try? JSONSerialization.jsonObject(with: d)) as? [String: Any],
              let url = o["url"] as? String, !url.isEmpty else { return }
        let p = PlayerViewController()
        p.urls = [url, (o["url2"] as? String) ?? ""].filter { !$0.isEmpty }
        p.titleText = (o["title"] as? String) ?? ""
        p.subText = (o["sub"] as? String) ?? ""
        p.isLive = (o["live"] as? Bool) ?? false
        p.startMs = (o["start"] as? NSNumber)?.int32Value ?? 0
        p.onDone = { [weak self] pos, dur, ended in
            self?.web.evaluateJavaScript("window.__appPlayerDone && window.__appPlayerDone(\(pos), \(dur), \(ended))", completionHandler: nil)
        }
        p.modalPresentationStyle = .fullScreen
        present(p, animated: true)
    }

    // MARK: navegação
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let u = navigationAction.request.url, navigationAction.navigationType == .linkActivated,
           let h = u.host, h != siteURL.host {
            UIApplication.shared.open(u)          // links de fora abrem no app certo
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        showError()
    }

    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let u = navigationAction.request.url { UIApplication.shared.open(u) }
        return nil
    }

    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let a = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        a.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler() })
        present(a, animated: true)
    }

    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        let a = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        a.addAction(UIAlertAction(title: "Cancelar", style: .cancel) { _ in completionHandler(false) })
        a.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler(true) })
        present(a, animated: true)
    }

    // MARK: sem internet
    private func showError() {
        guard errorView == nil else { return }
        let box = UIView(frame: view.bounds)
        box.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        box.backgroundColor = brand
        let label = UILabel()
        label.text = "Não foi possível abrir o app.\nConfira a internet e tente de novo."
        label.textColor = .white
        label.numberOfLines = 0
        label.textAlignment = .center
        label.font = .systemFont(ofSize: 18, weight: .medium)
        let btn = UIButton(type: .system)
        btn.setTitle("Tentar de novo", for: .normal)
        btn.titleLabel?.font = .systemFont(ofSize: 18, weight: .semibold)
        btn.setTitleColor(.black, for: .normal)
        btn.backgroundColor = .white
        btn.layer.cornerRadius = 12
        btn.contentEdgeInsets = UIEdgeInsets(top: 12, left: 24, bottom: 12, right: 24)
        btn.addTarget(self, action: #selector(retry), for: .touchUpInside)
        let st = UIStackView(arrangedSubviews: [label, btn])
        st.axis = .vertical
        st.spacing = 24
        st.alignment = .center
        st.translatesAutoresizingMaskIntoConstraints = false
        box.addSubview(st)
        NSLayoutConstraint.activate([
            st.centerXAnchor.constraint(equalTo: box.centerXAnchor),
            st.centerYAnchor.constraint(equalTo: box.centerYAnchor),
            st.leadingAnchor.constraint(greaterThanOrEqualTo: box.leadingAnchor, constant: 24)
        ])
        view.addSubview(box)
        errorView = box
    }

    @objc private func retry() {
        errorView?.removeFromSuperview()
        errorView = nil
        web.load(URLRequest(url: siteURL))
    }
}
