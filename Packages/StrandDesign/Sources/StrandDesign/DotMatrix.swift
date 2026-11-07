import SwiftUI

/// The dot-matrix numeral: hero metrics rendered as white dots on a regular grid, in the
/// spirit of the Doto typeface. `DotMatrix` holds the pure glyph tables so the layout can
/// be pinned byte-identically across platforms; `DotNumber` draws the resulting grid.
public enum DotMatrix {
    /// Columns of a single glyph; the character advance adds one blank gap column.
    public static let glyphColumns = 3
    public static let glyphAdvance = 4
    /// Rows of every glyph — the grid height in cell units.
    public static let rows = 7

    // Row masks, one per glyph row. Bit 2 (0b100) is the leftmost column, bit 1 the middle,
    // bit 0 the rightmost.
    private static let glyphs: [Character: [UInt8]] = [
        "0": [7, 5, 5, 5, 5, 5, 7],
        "1": [2, 6, 2, 2, 2, 2, 7],
        "2": [7, 1, 7, 6, 4, 4, 7],
        "3": [7, 1, 1, 7, 1, 1, 7],
        "4": [5, 5, 5, 7, 1, 1, 1],
        "5": [7, 4, 7, 1, 1, 1, 7],
        "6": [7, 4, 7, 5, 5, 5, 7],
        "7": [7, 1, 1, 2, 4, 4, 4],
        "8": [7, 5, 5, 7, 5, 5, 7],
        "9": [7, 5, 5, 7, 1, 1, 7],
        ",": [0, 0, 0, 0, 0, 7, 7],
        ".": [0, 0, 0, 0, 0, 7, 7],
        ":": [0, 2, 0, 0, 2, 0, 0],
        "-": [0, 0, 0, 7, 0, 0, 0],
        "–": [0, 0, 0, 7, 0, 0, 0],
        "+": [4, 4, 4, 7, 4, 4, 4],
    ]

    /// The dot grid for `text`: `rows` rows of 0/1 cells, one glyph cell per character plus a
    /// blank gap column between glyphs, so `count` characters span `count * glyphAdvance - 1`
    /// columns. Empty text yields `rows` empty rows; unknown characters render as blank blocks.
    public static func grid(_ text: String) -> [[Int]] {
        var columns = 0
        for _ in text { columns += glyphAdvance }
        if columns > 0 { columns -= 1 }
        var out = [[Int]](repeating: [Int](repeating: 0, count: columns), count: rows)
        var cell = 0
        for character in text {
            if let masks = glyphs[character] {
                for (row, mask) in masks.enumerated() {
                    for col in 0..<glyphColumns where mask & (0b100 >> col) != 0 {
                        out[row][cell + col] = 1
                    }
                }
            }
            cell += glyphAdvance
        }
        return out
    }
}

/// A dot-matrix numeral for hero metrics. `dot` is the dot diameter in points; each cell is
/// twice that, so a two-character value like "65" spans 15 cells wide and 7 cells high.
public struct DotNumber: View {
    public var text: String
    public var dot: CGFloat
    public var color: Color

    public init(text: String, dot: CGFloat = 4, color: Color = .primary) {
        self.text = text
        self.dot = dot
        self.color = color
    }

    public var body: some View {
        let grid = DotMatrix.grid(text)
        let columns = grid.first?.count ?? 0
        let step = dot * 2
        Canvas { context, _ in
            for (row, rowCells) in grid.enumerated() {
                for (col, on) in rowCells.enumerated() where on == 1 {
                    let rect = CGRect(
                        x: CGFloat(col) * step + (step - dot) / 2,
                        y: CGFloat(row) * step + (step - dot) / 2,
                        width: dot,
                        height: dot
                    )
                    context.fill(Path(ellipseIn: rect), with: .color(color))
                }
            }
        }
        .frame(width: step * CGFloat(columns), height: step * CGFloat(DotMatrix.rows))
        .accessibilityLabel(text)
    }
}
