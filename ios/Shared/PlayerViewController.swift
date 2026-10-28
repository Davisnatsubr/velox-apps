import UIKit
import MobileVLCKit

/// Player em tela cheia com VLC: toca HLS, TS, MP4, MKV, AC3, HEVC...
/// Tenta a DNS reserva sozinho e devolve ao site onde o vídeo parou.
final class PlayerViewController: UIViewController {

    var urls: [String] = []
    var titleText = ""
    var subText = ""
    var isLive = false
    var startMs: Int32 = 0
    var onDone: ((Double, Double, Bool) -> Void)?

    private let player = VLCMediaPlayer()
    private let videoView = UIView()
    private let topBar = UIView()
    private let bottomBar = UIView()
    private let titleLabel = UILabel()
    private let subLabel = UILabel()
    private let playBtn = UIButton(type: .system)
    private let aspectBtn = UIButton(type: .system)
    private let slider = UISlider()
    private let curLabel = UILabel()
    private let remLabel = UILabel()
    private let msgLabel = UILabel()
    private let spinner = UIActivityIndicatorView(style: .whiteLarge)

    private var idx = 0
    private var seekedStart = false
    private var ended = false
    private var closed = false
    private var dragging = false
    private var lastState: VLCMediaPlayerState = .stopped
    private var lastPos = 0.0
    private var lastDur = 0.0
    private var openedAt = Date()
    private var tickTimer: Timer?
    private var hideTimer: Timer?
    private var resizeIdx = UserDefaults.standard.integer(forKey: "resize") % 3
    private let resizeNames = ["Original", "Esticar", "Zoom"]

    override var prefersStatusBarHidden: Bool { true }
    override var prefersHomeIndicatorAutoHidden: Bool { true }
    override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .landscape }
    override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation { .landscapeRight }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        videoView.frame = view.bounds
        videoView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        videoView.backgroundColor = .black
        view.addSubview(videoView)
        player.drawable = videoView
        // o VLC desenha numa camada própria que "engole" os toques; quem recebe o toque é uma camada transparente por cima
        videoView.isUserInteractionEnabled = false
        let catcher = UIView(frame: view.bounds)
        catcher.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        catcher.backgroundColor = .clear
        view.addSubview(catcher)
        catcher.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(toggleControls)))
        let swipe = UISwipeGestureRecognizer(target: self, action: #selector(closeTapped))
        swipe.direction = .down
        catcher.addGestureRecognizer(swipe)
        buildUI()
        tickTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in self?.tick() }
        play(0)
        showControls()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        applyResize()
    }

    // MARK: reprodução
    private func play(_ i: Int) {
        guard i < urls.count, let u = URL(string: urls[i]) else { fail(); return }
        idx = i
        openedAt = Date()
        let m = VLCMedia(url: u)
        m.addOption(":network-caching=\(isLive ? 3000 : 1500)")
        m.addOption(":http-reconnect")
        m.addOption(":http-user-agent=VeloxApp iOS")
        player.media = m
        player.play()
        msgLabel.isHidden = true
        spinner.startAnimating()
    }

    private func tick() {
        let st = player.state
        if st != lastState {
            lastState = st
            switch st {
            case .error:
                if idx + 1 < urls.count { play(idx + 1) } else { fail() }
            case .ended:
                if !isLive && lastDur > 0 && lastPos > lastDur - 30 { ended = true; close() }
                else if idx + 1 < urls.count && lastPos < 1 { play(idx + 1) } else if lastPos < 1 { fail() }
            default: break
            }
        }
        // abriu, mas não saiu nenhum quadro em 25 s: tenta a reserva
        if !player.isPlaying && player.time.intValue == 0 && Date().timeIntervalSince(openedAt) > 25 && st != .paused {
            if idx + 1 < urls.count { play(idx + 1) } else if msgLabel.isHidden { fail() }
        }
        if player.isPlaying {
            spinner.stopAnimating()
            if !seekedStart && startMs > 5000 && !isLive {
                seekedStart = true
                player.time = VLCTime(int: startMs)
            }
        }
        let t = Double(player.time.intValue) / 1000
        let d = Double(player.media?.length.intValue ?? 0) / 1000
        if t > 0 { lastPos = t }
        if d > 0 { lastDur = d }
        if !isLive && !dragging && d > 0 { slider.value = Float(t / d) }
        curLabel.text = isLive ? "" : fmt(t)
        remLabel.text = (isLive || d <= 0) ? "" : "-" + fmt(max(0, d - t))
        playBtn.setImage(UIImage(systemName: player.isPlaying ? "pause.fill" : "play.fill"), for: .normal)
    }

    private func fail() {
        spinner.stopAnimating()
        msgLabel.text = "Não foi possível reproduzir.\nO conteúdo pode estar fora do ar ou no limite de telas.\nToque em ✕ para voltar."
        msgLabel.isHidden = false
        showControls()
    }

    private func close() {
        guard !closed else { return }
        closed = true
        tickTimer?.invalidate()
        hideTimer?.invalidate()
        let pos = isLive ? 0 : lastPos
        let dur = isLive ? 0 : lastDur
        player.stop()
        onDone?(pos, dur, ended)
        dismiss(animated: true)
    }

    private func fmt(_ s: Double) -> String {
        let v = Int(s)
        return v >= 3600 ? String(format: "%d:%02d:%02d", v / 3600, (v % 3600) / 60, v % 60) : String(format: "%02d:%02d", v / 60, v % 60)
    }

    // MARK: formato da tela (Original / Esticar / Zoom)
    private func applyResize() {
        let w = Int(videoView.bounds.width * UIScreen.main.scale)
        let h = Int(videoView.bounds.height * UIScreen.main.scale)
        guard w > 0, h > 0 else { return }
        let ratio = "\(w):\(h)"
        switch resizeIdx {
        case 1: player.videoCropGeometry = nil; player.videoAspectRatio = strdup(ratio)
        case 2: player.videoAspectRatio = nil; player.videoCropGeometry = strdup(ratio)
        default: player.videoAspectRatio = nil; player.videoCropGeometry = nil
        }
        aspectBtn.setTitle("  Tela: " + resizeNames[resizeIdx], for: .normal)
    }

    @objc private func cycleResize() {
        resizeIdx = (resizeIdx + 1) % 3
        UserDefaults.standard.set(resizeIdx, forKey: "resize")
        applyResize()
        showControls()
    }

    // MARK: áudio e legenda
    @objc private func pickAudio() { pickTrack(names: player.audioTrackNames, indexes: player.audioTrackIndexes, current: player.currentAudioTrackIndex, title: "Áudio") { self.player.currentAudioTrackIndex = $0 } }
    @objc private func pickSubs() { pickTrack(names: player.videoSubTitlesNames, indexes: player.videoSubTitlesIndexes, current: player.currentVideoSubTitleIndex, title: "Legenda") { self.player.currentVideoSubTitleIndex = $0 } }

    private func pickTrack(names: [Any], indexes: [Any], current: Int32, title: String, set: @escaping (Int32) -> Void) {
        let a = UIAlertController(title: title, message: nil, preferredStyle: .actionSheet)
        for (i, n) in names.enumerated() {
            guard i < indexes.count, let ix = (indexes[i] as? NSNumber)?.int32Value else { continue }
            let label = (n as? String) ?? "Faixa \(i + 1)"
            a.addAction(UIAlertAction(title: (ix == current ? "✓ " : "") + label, style: .default) { _ in set(ix) })
        }
        if names.isEmpty { a.message = "Nenhuma opção neste vídeo." }
        a.addAction(UIAlertAction(title: "Fechar", style: .cancel))
        if let pop = a.popoverPresentationController { pop.sourceView = topBar; pop.sourceRect = CGRect(x: topBar.bounds.maxX - 120, y: topBar.bounds.midY, width: 1, height: 1) }
        present(a, animated: true)
    }

    // MARK: controles
    @objc private func togglePlay() { player.isPlaying ? player.pause() : player.play(); showControls() }
    @objc private func back10() { player.jumpBackward(10); showControls() }
    @objc private func fwd10() { player.jumpForward(10); showControls() }
    @objc private func closeTapped() { close() }
    @objc private func sliderDown() { dragging = true; hideTimer?.invalidate() }
    @objc private func sliderUp() { player.position = slider.value; dragging = false; showControls() }

    @objc private func toggleControls() { topBar.alpha > 0.5 ? hideControls() : showControls() }

    private func showControls() {
        UIView.animate(withDuration: 0.2) { self.topBar.alpha = 1; self.bottomBar.alpha = 1 }
        hideTimer?.invalidate()
        hideTimer = Timer.scheduledTimer(withTimeInterval: 4, repeats: false) { [weak self] _ in
            guard let s = self, s.player.isPlaying, !s.dragging else { return }
            s.hideControls()
        }
    }

    private func hideControls() { UIView.animate(withDuration: 0.25) { self.topBar.alpha = 0; self.bottomBar.alpha = 0 } }

    private func iconButton(_ name: String, _ action: Selector, size: CGFloat = 22) -> UIButton {
        let b = UIButton(type: .system)
        b.setImage(UIImage(systemName: name, withConfiguration: UIImage.SymbolConfiguration(pointSize: size, weight: .semibold)), for: .normal)
        b.tintColor = .white
        b.addTarget(self, action: action, for: .touchUpInside)
        b.widthAnchor.constraint(equalToConstant: 44).isActive = true
        b.heightAnchor.constraint(equalToConstant: 44).isActive = true
        return b
    }

    private func buildUI() {
        for (bar, top) in [(topBar, true), (bottomBar, false)] {
            bar.translatesAutoresizingMaskIntoConstraints = false
            let g = CAGradientLayer()
            g.colors = [UIColor(white: 0, alpha: 0.75).cgColor, UIColor.clear.cgColor]
            if !top { g.startPoint = CGPoint(x: 0.5, y: 1); g.endPoint = CGPoint(x: 0.5, y: 0) }
            g.frame = CGRect(x: 0, y: 0, width: 4000, height: 120)
            bar.layer.insertSublayer(g, at: 0)
            view.addSubview(bar)
        }
        let safe = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            topBar.topAnchor.constraint(equalTo: view.topAnchor),
            topBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            topBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            topBar.bottomAnchor.constraint(equalTo: safe.topAnchor, constant: 64),
            bottomBar.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            bottomBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            bottomBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            bottomBar.topAnchor.constraint(equalTo: safe.bottomAnchor, constant: -70)
        ])

        // barra de cima: fechar, título, formato, áudio, legenda
        titleLabel.text = titleText
        titleLabel.textColor = .white
        titleLabel.font = .systemFont(ofSize: 18, weight: .semibold)
        subLabel.text = isLive ? "● AO VIVO" + (subText.isEmpty ? "" : "  ·  " + subText) : subText
        subLabel.textColor = isLive ? UIColor(red: 1, green: 0.3, blue: 0.43, alpha: 1) : UIColor(white: 0.75, alpha: 1)
        subLabel.font = .systemFont(ofSize: 13, weight: .medium)
        subLabel.isHidden = subLabel.text?.isEmpty ?? true
        let titles = UIStackView(arrangedSubviews: [titleLabel, subLabel])
        titles.axis = .vertical
        aspectBtn.setImage(UIImage(systemName: "aspectratio"), for: .normal)
        aspectBtn.tintColor = .white
        aspectBtn.setTitleColor(.white, for: .normal)
        aspectBtn.titleLabel?.font = .systemFont(ofSize: 14, weight: .semibold)
        aspectBtn.addTarget(self, action: #selector(cycleResize), for: .touchUpInside)
        let top = UIStackView(arrangedSubviews: [iconButton("xmark", #selector(closeTapped)), titles, aspectBtn,
                                                 iconButton("speaker.wave.2", #selector(pickAudio), size: 18),
                                                 iconButton("captions.bubble", #selector(pickSubs), size: 18)])
        top.spacing = 12
        top.alignment = .center
        top.translatesAutoresizingMaskIntoConstraints = false
        topBar.addSubview(top)
        NSLayoutConstraint.activate([
            top.leadingAnchor.constraint(equalTo: safe.leadingAnchor, constant: 12),
            top.trailingAnchor.constraint(equalTo: safe.trailingAnchor, constant: -12),
            top.bottomAnchor.constraint(equalTo: topBar.bottomAnchor, constant: -8)
        ])

        // barra de baixo: -10, play, +10, tempo, barra, restante
        playBtn.tintColor = .white
        playBtn.addTarget(self, action: #selector(togglePlay), for: .touchUpInside)
        playBtn.widthAnchor.constraint(equalToConstant: 48).isActive = true
        for l in [curLabel, remLabel] {
            l.textColor = .white
            l.font = .monospacedDigitSystemFont(ofSize: 13, weight: .medium)
        }
        slider.minimumTrackTintColor = UIColor(red: 0.24, green: 0.86, blue: 0.71, alpha: 1)
        slider.addTarget(self, action: #selector(sliderDown), for: .touchDown)
        slider.addTarget(self, action: #selector(sliderUp), for: [.touchUpInside, .touchUpOutside, .touchCancel])
        var items: [UIView] = []
        if !isLive { items.append(iconButton("gobackward.10", #selector(back10))) }
        items.append(playBtn)
        if !isLive { items += [iconButton("goforward.10", #selector(fwd10)), curLabel, slider, remLabel] } else { items.append(UIView()) }
        let bottom = UIStackView(arrangedSubviews: items)
        bottom.spacing = 12
        bottom.alignment = .center
        bottom.translatesAutoresizingMaskIntoConstraints = false
        bottomBar.addSubview(bottom)
        NSLayoutConstraint.activate([
            bottom.leadingAnchor.constraint(equalTo: safe.leadingAnchor, constant: 12),
            bottom.trailingAnchor.constraint(equalTo: safe.trailingAnchor, constant: -16),
            bottom.topAnchor.constraint(equalTo: bottomBar.topAnchor, constant: 12)
        ])

        // carregando + mensagem de erro
        spinner.color = .white
        spinner.hidesWhenStopped = true
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        msgLabel.textColor = .white
        msgLabel.numberOfLines = 0
        msgLabel.textAlignment = .center
        msgLabel.font = .systemFont(ofSize: 17, weight: .medium)
        msgLabel.backgroundColor = UIColor(white: 0.08, alpha: 0.9)
        msgLabel.layer.cornerRadius = 14
        msgLabel.clipsToBounds = true
        msgLabel.isHidden = true
        msgLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(msgLabel)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            msgLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            msgLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            msgLabel.widthAnchor.constraint(lessThanOrEqualToConstant: 520),
            msgLabel.heightAnchor.constraint(greaterThanOrEqualToConstant: 120)
        ])
        view.bringSubviewToFront(topBar)
        view.bringSubviewToFront(bottomBar)
    }
}
